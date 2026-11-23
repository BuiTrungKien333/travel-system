// Runs only for a new MongoDB volume; credentials are supplied through private environment.
const database = process.env.MONGODB_DATABASE || "wego_telemetry";
db.getSiblingDB(database).createUser({user: process.env.MONGO_APP_USER, pwd: process.env.MONGO_APP_PASSWORD,
  roles: [{role: "readWrite", db: database}, {role: "dbAdmin", db: database}]});
