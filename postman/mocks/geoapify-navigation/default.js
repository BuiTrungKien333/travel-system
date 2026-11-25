const http = require('http');
const route = require('./route.json');
http.createServer((req, res) => {
  const url = new URL(req.url, 'http://localhost');
  res.setHeader('Content-Type', 'application/json');
  if (url.pathname !== '/v1/routing') {
    res.writeHead(501); res.end(JSON.stringify({ message: 'Outside the observed provider subset' })); return;
  }
  const query = url.searchParams;
  if (query.get('mode') !== 'drive' || query.get('lang') !== 'en'
      || query.get('waypoints') !== '21.0285,105.8542|21.0278,105.8342'
      || query.get('optimize_stops') !== 'false' || query.get('intermediate_waypoint_mode') !== 'stopover'
      || !['balanced', 'short'].includes(query.get('type'))
      || query.get('details') !== 'instruction_details,route_details,polyline5') {
    res.writeHead(400); res.end(JSON.stringify({ message: 'Request does not match the recorded contract fixture' })); return;
  }
  res.writeHead(200); res.end(JSON.stringify(route));
}).listen(process.env.PORT || 18765);
