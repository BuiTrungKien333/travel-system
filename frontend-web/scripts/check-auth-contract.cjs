const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');
const axios = require('axios');

function load(file, dependencies) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, esModuleInterop: true },
  });
  const module = { exports: {} };
  vm.runInNewContext(outputText, {
    module, exports: module.exports,
    require: (name) => name in dependencies ? dependencies[name] : require(name),
    process: { env: { NEXT_PUBLIC_ENABLE_MOCK: 'false' } },
  }, { filename: file });
  return module.exports;
}

async function check() {
  const mockService = load('src/lib/mock-service.ts', {
    './mock-data': { MOCK_USER: { id: 'user-1', email: 'user@example.com' } },
  }).mockService;
  const api = load('src/lib/api.ts', { './mock-service': { mockService } }).default;
  const user = { id: 'user-1', fullName: 'Test User', email: 'user@example.com' };
  const credentials = { user, accessToken: 'access', refreshToken: 'refresh' };
  const registration = { verificationRequired: true, resendAfterSeconds: 60 };
  let request;
  api.defaults.adapter = async (config) => {
    request = JSON.parse(config.data);
    return {
      config, status: 200, statusText: 'OK', headers: {},
      data: { success: true, data: config.url === '/auth/login' ? credentials : registration },
    };
  };

  let authenticated;
  let cached;
  const dependencies = {
    '@tanstack/react-query': {
      useMutation: (options) => options,
      useQueryClient: () => ({ setQueryData: (_, value) => { cached = value; } }),
    },
    '@/lib/api': { __esModule: true, default: api },
    '../store/useAuthStore': {
      useAuthStore: (select) => select({ setAuth: (...args) => { authenticated = args; } }),
    },
    '@/lib/constants': { QUERY_KEYS: { me: () => ['me'] } },
  };
  const login = load('src/features/auth/api/useLoginMutation.ts', dependencies).useLoginMutation();
  const result = await login.mutationFn({ email: user.email, password: 'Password123@' });
  login.onSuccess(result);
  assert.equal(authenticated[0], user);
  assert.equal(authenticated[1], credentials.accessToken);
  assert.equal(authenticated[2], credentials.refreshToken);
  assert.equal(cached, user);

  authenticated = undefined;
  const register = load('src/features/auth/api/useRegisterMutation.ts', dependencies).useRegisterMutation();
  const accepted = await register.mutationFn({ ...user, password: 'Password123@', confirmPassword: 'Password123@' });
  if (register.onSuccess) register.onSuccess(accepted);
  assert.equal(accepted.verificationRequired, true);
  assert.equal(authenticated, undefined, 'Registration must not create an authenticated session');
  assert.deepEqual(Object.keys(request).sort(), ['email', 'fullName', 'password']);
  assert.equal((await mockService.login(user.email)).accessToken, 'mock-jwt-token-12345');
  assert.equal((await mockService.register()).verificationRequired, true);

  let cleared = false;
  let cacheCleared = false;
  const logout = load('src/features/auth/api/useLogoutMutation.ts', {
    ...dependencies,
    '@tanstack/react-query': {
      useMutation: (options) => options,
      useQueryClient: () => ({ clear: () => { cacheCleared = true; } }),
    },
    '../store/useAuthStore': {
      useAuthStore: (select) => select({ refreshToken: credentials.refreshToken, clearAuth: () => { cleared = true; } }),
    },
  }).useLogoutMutation();
  await logout.mutationFn();
  assert.deepEqual(request, { refreshToken: credentials.refreshToken });
  logout.onSettled();
  assert.equal(cleared && cacheCleared, true);

  api.defaults.adapter = async (config) => { throw new axios.AxiosError('Forbidden', '', config, null, {
    config, status: 403, data: { success: false, code: 'AUTH_008', message: 'Verify email' },
  }); };
  await assert.rejects(api.post('/auth/login', {}), (error) => error.status === 403 && error.code === 'AUTH_008');
  console.log('PASS: login tokens/cache, registration without auto-login, logout revocation, mock contracts, API error status');
}

check().catch((error) => { console.error(error); process.exitCode = 1; });
