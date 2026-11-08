import type { QueryClient } from '@tanstack/react-query';

export function invalidateMemories(client: QueryClient) {
  return Promise.all([
    client.invalidateQueries({ queryKey: ['memories'] }),
    client.invalidateQueries({ queryKey: ['journal'] }),
    client.invalidateQueries({ queryKey: ['me', 'memories'] }),
    client.invalidateQueries({ queryKey: ['me', 'trips-with-memories'] }),
    client.invalidateQueries({ queryKey: ['trips'] }),
    client.invalidateQueries({ queryKey: ['journey'] }),
  ]);
}
