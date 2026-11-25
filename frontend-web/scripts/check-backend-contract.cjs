const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");
const axios = require("axios");

const root = path.join(__dirname, "..");
const modules = new Map();
const clients = [];
const state = {
  token: null,
  refreshToken: null,
  user: null,
  setAuth(user, token, refreshToken) {
    Object.assign(state, { user, token, refreshToken });
  },
  clearAuth() {
    Object.assign(state, { user: null, token: null, refreshToken: null });
  },
};
const store = Object.assign((select) => select(state), {
  getState: () => state,
});
const cache = new Map();
const queryClient = {
  getQueryData: (key) => cache.get(JSON.stringify(key)),
  setQueryData: (key, value) => cache.set(JSON.stringify(key), value),
  invalidateQueries() {},
  removeQueries() {},
  clear() {
    cache.clear();
  },
};
const ui = { setPendingRecommendation() {}, setIsGenerating() {} };
let retainedConnectionRef;
const mocks = {
  react: {
    useRef: (value) => retainedConnectionRef ?? { current: value },
    useEffect() {},
  },
  "@tanstack/react-query": {
    useQuery: (options) => options,
    useMutation: (options) => options,
    useQueryClient: () => queryClient,
  },
  "@/features/auth/store/useAuthStore": { useAuthStore: store },
  "../store/useAuthStore": { useAuthStore: store },
  "../store/useItineraryUIStore": {
    useItineraryUIStore: (select) => select(ui),
  },
  axios: new Proxy(axios, {
    get(target, key) {
      if (key !== "create") return target[key];
      return (options) => {
        const client = axios.create(options);
        clients.push(client);
        return client;
      };
    },
  }),
};
function load(file) {
  file = path.resolve(root, file);
  if (modules.has(file)) return modules.get(file).exports;
  const module = { exports: {} };
  modules.set(file, module);
  const source = ts.transpileModule(fs.readFileSync(file, "utf8"), {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2020,
      esModuleInterop: true,
    },
  }).outputText;
  vm.runInNewContext(
    source,
    {
      module,
      exports: module.exports,
      process,
      FormData,
      File,
      Blob,
      crypto,
      console,
      require(name) {
        if (name in mocks) return mocks[name];
        if (name.startsWith("@/") || name.startsWith(".")) {
          let candidate = name.startsWith("@/")
            ? path.join(root, "src", name.slice(2))
            : path.resolve(path.dirname(file), name);
          candidate = fs.existsSync(`${candidate}.ts`)
            ? `${candidate}.ts`
            : path.join(candidate, "index.ts");
          return load(candidate);
        }
        return require(name);
      },
    },
    { filename: file },
  );
  return module.exports;
}
const api = load("src/lib/api.ts").default;
const backend = load("src/lib/backend.ts");
const hook = (feature, name, ...args) =>
  load(`src/features/${feature}/api/${name}.ts`)[name](...args);
const reply = (config, data) => ({
  config,
  data: { success: true, data },
  status: 200,
  statusText: "OK",
  headers: {},
});
const trip = {
  id: "trip-1",
  title: "Đà Nẵng",
  destination: { name: "Đà Nẵng" },
  startDate: "2026-12-01",
  endDate: "2026-12-02",
  status: "PLANNING",
  mode: "PLANNING",
  stopCount: 2,
  memoryCount: 0,
  version: 3,
};
const input = {
  clientRequestId: "request-1",
  name: "Đà Nẵng",
  destination: "Đà Nẵng",
  startDate: trip.startDate,
  endDate: trip.endDate,
  preferences: ["Food"],
  budget: 1200000,
  transportation: "Car",
};
const itinerary = {
  id: "itinerary-1",
  tripId: trip.id,
  name: trip.title,
  status: "DRAFT",
  updatedAt: "2026-11-25T00:00:00Z",
  days: [
    {
      date: trip.startDate,
      stops: [
        {
          id: "stop-1",
          placeId: "place-1",
          name: "Bảo tàng",
          sequence: 0,
          arrivalTime: `${trip.startDate}T09:00:00`,
          durationMinutes: 60,
        },
      ],
    },
  ],
};
async function contractChecks() {
  let requests = [];
  api.defaults.adapter = async (config) => {
    requests.push(config);
    if (config.url === "/trips" && config.method === "get")
      return reply(config, {
        content: [{ ...trip, id: `trip-${config.params.page}` }],
        totalPages: 2,
      });
    if (config.url === "/trips" || config.url === `/trips/${trip.id}`)
      return reply(config, { trip });
    return reply(config, itinerary);
  };
  assert.equal(
    (await backend.fetchTrips()).length,
    2,
    "Pagination must not drop later pages",
  );
  assert.equal(requests[1].params.page, 1);
  assert.equal(backend.mapTrip(trip).name, trip.title);
  const create = hook("trip", "useCreateTripMutation");
  await create.mutationFn(input);
  const payload = JSON.parse(requests.at(-1).data);
  assert.equal(payload.clientRequestId, input.clientRequestId);
  assert.equal(payload.title, input.name);
  assert.equal(payload.destination.name, input.destination);
  assert.equal(payload.budget.currency, "VND");
  assert.deepEqual(payload.preferences, ["food"]);
  assert.equal(
    "transportation" in payload,
    false,
    "Do not send unsupported trip fields",
  );
  cache.set(JSON.stringify(["trips", trip.id]), backend.mapTrip(trip));
  await hook("trip", "useUpdateTripMutation", trip.id).mutationFn({
    name: "Updated",
  });
  assert.equal(requests.at(-1).method, "patch");
  assert.equal(JSON.parse(requests.at(-1).data).version, 3);
  await hook("trip", "useDeleteTripMutation").mutationFn(trip.id);
  assert.equal(requests.at(-1).params.version, 3);
  const addToDay = hook("place", "useAddPlaceToDayMutation", trip.id);
  await addToDay.mutationFn({ placeId: "place-1", date: trip.endDate });
  assert.equal(requests.at(-1).url, `/trips/${trip.id}/places/batch`);
  assert.deepEqual(JSON.parse(requests.at(-1).data), {
    placeIds: ["place-1"],
    date: trip.endDate,
  });
  await assert.rejects(
    addToDay.mutationFn({ placeId: "place-1", date: "" }),
    /chọn ngày/,
  );
  const navigation = load("src/features/map/navigation.ts");
  const navPayload = navigation.navigationPayload(
    trip.id,
    {
      date: trip.endDate,
      nextStopIndex: 1,
      expectedPlaceIds: ["place-1", "place-2"],
      vehicle: "TWO_WHEELER",
    },
    { latitude: 16, longitude: 108 },
  );
  assert.equal(navPayload.trip.nextStopIndex, 1);
  assert.equal(navPayload.trip.expectedPlaceIds.length, 2);
  assert.equal(navPayload.vehicle, "TWO_WHEELER");
  assert.equal("destinations" in navPayload, false);
  assert.throws(
    () =>
      navigation.navigationPayload(
        trip.id,
        {
          date: trip.endDate,
          nextStopIndex: 2,
          expectedPlaceIds: ["place-1"],
          vehicle: "DRIVE",
        },
        { latitude: 16, longitude: 108 },
      ),
    /hợp lệ/,
  );
  assert.equal(
    navigation.maneuverText({ type: "Right", streets: ["Đường A"] }),
    "Rẽ phải — Đường A",
  );
  assert.equal(
    navigation.requireGeoapifyRoute({ provider: "geoapify-routing" }).provider,
    "geoapify-routing",
  );
  assert.throws(
    () => navigation.requireGeoapifyRoute({ provider: "google" }),
    /Geoapify hợp lệ/,
  );
  const mapped = backend.mapItinerary(itinerary);
  assert.equal(mapped.days[0].items[0].suggestedStartTime, "09:00");
  assert.equal(
    backend.itineraryPayload(mapped).days[0].stops[0].arrivalTime,
    `${trip.startDate}T09:00:00`,
  );
  assert.throws(() => backend.itineraryPayload({}), /Thiếu/);
  const proposal = {
    status: "ALL_CLEAR",
    totalTripDistanceKm: 2,
    totalTripTransitMinutes: 10,
    dailySchedules: [],
  };
  await hook("itinerary", "useUpdateItineraryMutation", trip.id).mutationFn({
    optimization: { sourceUpdatedAt: itinerary.updatedAt, proposal },
  });
  assert.equal(
    requests.at(-1).url,
    `/trips/${trip.id}/itinerary/apply-optimization`,
  );
  assert.deepEqual(JSON.parse(requests.at(-1).data), {
    expectedUpdatedAt: itinerary.updatedAt,
    proposal,
  });
  const reorder = load("src/features/itinerary/reorder.ts").reorderItinerary;
  const twoDays = {
    ...mapped,
    days: [...mapped.days, { date: trip.endDate, dayIndex: 1, items: [] }],
  };
  const moved = reorder(twoDays, "stop-1", "day-1");
  assert.equal(moved.days[0].items.length, 0);
  assert.equal(moved.days[1].items[0].dayIndex, 1);
  assert.equal(
    moved.days[1].items[0].suggestedStartTime,
    undefined,
    "Moving a stop clears its old time",
  );
  assert.equal(moved.days[1].items[0].scheduledArrival, undefined);
  const overnight = backend.mapItinerary({
    ...itinerary,
    days: [
      {
        ...itinerary.days[0],
        stops: [
          { ...itinerary.days[0].stops[0], arrivalTime: "2026-12-02T00:30:15" },
        ],
      },
    ],
  });
  assert.equal(
    backend.itineraryPayload(overnight).days[0].stops[0].arrivalTime,
    "2026-12-02T00:30:15",
  );
  assert.equal(
    reorder({ ...twoDays, readOnly: true }, "stop-1", "day-1"),
    null,
  );
  assert.equal(
    reorder({ ...twoDays, editableFrom: "9999-01-01" }, "stop-1", "day-1"),
    null,
  );
  await hook("itinerary", "useUpdateItineraryMutation", trip.id).mutationFn({
    ...mapped,
    tripVersion: 7,
  });
  assert.equal(JSON.parse(requests.at(-1).data).expectedTripVersion, 7);
  assert.equal(
    JSON.parse(requests.at(-1).data).expectedUpdatedAt,
    itinerary.updatedAt,
  );
  assert.equal(twoDays.days[0].items.length, 1, "Do not mutate cached data");
  const connections = load("src/features/itinerary/connections.ts");
  const connectedDay = {
    ...twoDays,
    days: twoDays.days.map((day, index) => ({
      ...day,
      items: [
        {
          ...mapped.days[0].items[0],
          id: `a-${index}`,
          placeId: `a-${index}`,
          placeLat: 10,
          placeLng: 106,
        },
        {
          ...mapped.days[0].items[0],
          id: `b-${index}`,
          placeId: `b-${index}`,
          placeLat: 10.01,
          placeLng: 106.01,
        },
      ],
    })),
  };
  const estimated = connections.itineraryRouteGroups(connectedDay);
  assert.equal(estimated.length, 2);
  assert.notEqual(estimated[0].color, estimated[1].color);
  assert.equal(
    estimated[0].geometry[0].length,
    2,
    "Connect each day separately, never bridge days",
  );
  assert.equal(estimated[0].estimated, true);
  const realGeometry = [
    { latitude: 10, longitude: 106 },
    { latitude: 10.005, longitude: 106.008 },
    { latitude: 10.01, longitude: 106.01 },
  ];
  const connected = {
    itinerary,
    days: [
      {
        date: connectedDay.days[0].date,
        route: { legs: [{ geometry: realGeometry }] },
      },
    ],
  };
  assert.equal(
    connections.itineraryRouteGroups(connectedDay, connected)[0].estimated,
    false,
  );
  assert.equal(
    connections.itineraryRouteGroups(connectedDay, connected)[0].geometry[0]
      .length,
    3,
  );
  assert.equal(
    connections.itineraryRouteGroups(
      { ...connectedDay, updatedAt: "new-revision" },
      connected,
    )[0].estimated,
    true,
    "Do not draw a stale road route",
  );
  api.defaults.adapter = async (config) => {
    requests.push(config);
    return reply(config, { itinerary, days: [] });
  };
  const connectQuery = hook(
    "itinerary",
    "useItineraryConnectionsQuery",
    trip.id,
    { ...connectedDay, tripVersion: 7 },
  );
  await connectQuery.queryFn({ signal: new AbortController().signal });
  assert.equal(requests.at(-1).url, `/trips/${trip.id}/itinerary/connect`);
  assert.equal(JSON.parse(requests.at(-1).data).optimize, true);
  assert.equal(JSON.parse(requests.at(-1).data).expectedTripVersion, 7);
  await connectQuery.queryFn({ signal: new AbortController().signal });
  assert.equal(
    JSON.parse(requests.at(-1).data).optimize,
    false,
    "Subsequent route refreshes preserve manual order",
  );
  retainedConnectionRef = { current: "" };
  const manualQuery = hook(
    "itinerary",
    "useItineraryConnectionsQuery",
    trip.id,
    connectedDay,
  );
  manualQuery.preserveManualOrder(moved);
  const afterManualDayMove = hook(
    "itinerary",
    "useItineraryConnectionsQuery",
    trip.id,
    moved,
  );
  await afterManualDayMove.queryFn({ signal: new AbortController().signal });
  assert.equal(
    JSON.parse(requests.at(-1).data).optimize,
    false,
    "A manual move between days is not optimized away",
  );
  retainedConnectionRef = undefined;
  const memory = { id: "memory-1", tripId: trip.id, mediaCount: 0, media: [] };
  api.defaults.adapter = async (config) => {
    requests.push(config);
    if (config.url === "/media/uploads")
      return reply(config, { succeeded: 0, failed: 1 });
    return reply(config, memory);
  };
  const upload = hook("memory", "useCreateMemoryMutation", trip.id);
  const memoryInput = {
    clientRequestId: "stable-memory-id",
    note: "Test",
    files: [new File(["file"], "photo.png", { type: "image/png" })],
  };
  await assert.rejects(upload.mutationFn(memoryInput), /chưa tải được/);
  await assert.rejects(upload.mutationFn(memoryInput), /chưa tải được/);
  const creates = requests.filter((r) => r.url === "/journal/memories");
  assert.equal(
    JSON.parse(creates[0].data).clientRequestId,
    JSON.parse(creates[1].data).clientRequestId,
  );
  const mediaRequest = requests.at(-1);
  assert.equal(mediaRequest.params.memoryId, memory.id);
  assert.ok(mediaRequest.data instanceof FormData);
  assert.notEqual(mediaRequest.headers.getContentType(), "application/json");
  api.defaults.adapter = async (config) => {
    if (config.url === "/trips")
      return reply(config, { content: [trip], totalPages: 1 });
    if (config.url === "/journal/memories")
      return reply(config, {
        content: [
          {
            ...memory,
            placeId: "place-1",
            experiencedAt: "2026-11-25T00:00:00Z",
          },
          {
            ...memory,
            id: "standalone-memory",
            tripId: null,
            note: "Standalone",
            placeId: "unavailable-place",
            experiencedAt: "2026-11-24T00:00:00Z",
          },
        ],
        totalPages: 1,
      });
    if (config.url === "/home/dashboard")
      return reply(config, { footprint: { provinces: 1, places: 1 } });
    if (config.url === "/destinations")
      return reply(config, {
        content: [{ id: "province-1", code: "DN", name: "Đà Nẵng" }],
        totalPages: 1,
      });
    if (config.url === "/places/place-1")
      return reply(config, {
        id: "place-1",
        name: "Bảo tàng",
        provinceId: "province-1",
        location: { latitude: 16, longitude: 108 },
      });
    if (config.url === "/places/unavailable-place")
      throw new axios.AxiosError("Not found", "", config, null, {
        status: 404,
        data: { code: "PLACE_001", message: "Place not found" },
      });
    throw new Error(`Unexpected journal endpoint: ${config.url}`);
  };
  const journal = await load(
    "src/features/journal/api/useJournalDataQuery.ts",
  ).fetchJournalData();
  assert.equal(
    journal.journeys[0].locations[0].name,
    "Bảo tàng",
    "Memory history survives removal from trip-place membership",
  );
  assert.equal(journal.stats.exploredProvinces[0].name, "Đà Nẵng");
  assert.equal(
    journal.recentMemories.find((m) => m.id === "standalone-memory").location,
    "Địa điểm không còn khả dụng",
  );
  assert.equal(
    journal.recentMemories.length,
    2,
    "Standalone memories must remain visible",
  );
  assert.equal(
    journal.journeys.find((j) => j.id === "unassigned-memories").memories
      .length,
    1,
  );
  let refreshes = 0;
  state.setAuth({ id: "user-1" }, "expired", "refresh-1");
  clients[1].defaults.adapter = async (config) => {
    refreshes++;
    await new Promise((resolve) => setTimeout(resolve, 10));
    return reply(config, {
      user: { id: "user-1" },
      accessToken: "fresh",
      refreshToken: "refresh-2",
    });
  };
  api.defaults.adapter = async (config) => {
    if (config.headers.Authorization !== "Bearer fresh")
      throw new axios.AxiosError("Unauthorized", "", config, null, {
        status: 401,
        data: {},
      });
    return reply(config, { ok: true });
  };
  await Promise.all([api.get("/users/me"), api.get("/trips")]);
  assert.equal(refreshes, 1, "Parallel 401s must share one token refresh");
  assert.equal(state.token, "fresh");
  state.clearAuth();
  api.defaults.adapter = async (config) => {
    throw new axios.AxiosError("Conflict", "", config, null, {
      status: 409,
      data: { code: "TRIP_STALE", message: "Reload trip" },
    });
  };
  await assert.rejects(
    api.patch("/trips/trip-1", {}),
    (error) => error.status === 409 && error.code === "TRIP_STALE",
  );
  console.log(
    "PASS: pagination, trip payload/version, itinerary preview/apply, reorder, multipart partial failure/retry, shared refresh, conflict errors",
  );
}
async function liveChecks() {
  if (!process.env.API_TEST_EMAIL || !process.env.API_TEST_PASSWORD)
    throw new Error(
      "Set API_TEST_EMAIL and API_TEST_PASSWORD for a local test account",
    );
  let created;
  try {
    const { data: auth } = await api.post("/auth/login", {
      email: process.env.API_TEST_EMAIL,
      password: process.env.API_TEST_PASSWORD,
    });
    state.setAuth(auth.user, auth.accessToken, auth.refreshToken);
    const profile = await api.get("/users/me");
    assert.equal(profile.data.email, process.env.API_TEST_EMAIL);
    const before = await api.get("/trips");
    const start = new Date(`${before.data.context.evaluatedDate}T00:00:00Z`);
    start.setUTCDate(start.getUTCDate() + 7);
    const date = start.toISOString().slice(0, 10);
    const request = {
      clientRequestId: crypto.randomUUID(),
      name: `Frontend API check ${Date.now()}`,
      destination: "Đà Nẵng",
      startDate: date,
      endDate: date,
      preferences: ["Food"],
    };
    const create = hook("trip", "useCreateTripMutation");
    created = await create.mutationFn(request);
    assert.equal(
      (await create.mutationFn(request)).id,
      created.id,
      "Creation retry must be idempotent",
    );
    cache.set(JSON.stringify(["trips", created.id]), created);
    const updated = await hook(
      "trip",
      "useUpdateTripMutation",
      created.id,
    ).mutationFn({ name: `${request.name} updated` });
    assert.equal(updated.name, `${request.name} updated`);
    cache.set(JSON.stringify(["trips", created.id]), updated);
    assert.ok(
      (await backend.fetchTrips()).some((trip) => trip.id === created.id),
    );
    const empty = await hook(
      "itinerary",
      "useItineraryQuery",
      created.id,
    ).queryFn();
    assert.equal(
      empty.days.length,
      1,
      "An empty trip must still expose its calendar days",
    );
    const journal = await backend.fetchMemories();
    assert.ok(Array.isArray(journal));
    const dashboard = await api.get("/home/dashboard");
    assert.ok(dashboard.data.footprint);
    console.log(
      "PASS LIVE: login/profile, paginated trips, idempotent create, versioned patch, empty itinerary days, memories, dashboard",
    );
  } finally {
    if (created)
      await hook("trip", "useDeleteTripMutation").mutationFn(created.id);
    if (state.refreshToken)
      await api.post("/auth/logout", { refreshToken: state.refreshToken });
    state.clearAuth();
  }
}
(process.argv.includes("--live") ? liveChecks() : contractChecks()).catch(
  (error) => {
    console.error(error.message);
    process.exitCode = 1;
  },
);
