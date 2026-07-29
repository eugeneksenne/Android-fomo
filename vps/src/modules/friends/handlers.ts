import { Server as SocketIOServer, Socket } from 'socket.io';
import { z } from 'zod';
import { friendsEngine } from './friendsEngine.js';
import { validatePayload } from '../../core/middleware/validation.js';
import { generateCorrelationId } from '../../shared/utils/id.js';
import { AppError } from '../../shared/errors/appError.js';
import { ErrorCodes } from '../../shared/errors/errorCodes.js';

const friendsPresenceSchema = z.object({
  friendIds: z.array(z.string().min(3).max(128)).max(200).optional(),
  correlationId: z.string().optional(),
});

export function friendsHandlers(io: SocketIOServer, socket: Socket): void {
  const userId = (socket.data as any).user?.id as string;

  socket.on('friends:presence:get', async (payload: any, ack?: (res: any) => void) => {
    const correlationId = payload?.correlationId || generateCorrelationId();
    try {
      const data = validatePayload(friendsPresenceSchema, payload || {}, correlationId);
      const friendIds = data.friendIds || [];

      if (friendIds.length === 0) {
        if (typeof ack === 'function') ack({ success: true, data: { friends: [], categorized: {} }, correlationId, timestamp: new Date().toISOString() });
        return;
      }

      const presences = await friendsEngine.getFriendsPresence(friendIds);
      const categorized = friendsEngine.categorizeFriends(presences);

      const response = {
        friends: presences.map(p => ({
          userId: p.userId,
          status: p.status,
          lastSeen: p.lastSeen,
          displayName: p.displayName,
          photoUrl: p.photoUrl,
          isOnline: p.isOnline,
          venueId: p.venueId,
          customMessage: p.customMessage,
        })),
        categorized: {
          onlineCount: categorized.online.length,
          activeNowCount: categorized.activeNow.length,
          inCallCount: categorized.inCall.length,
          watchingLiveCount: categorized.watchingLive.length,
          insideVenueCount: categorized.insideVenue.length,
          walkingHomeCount: categorized.walkingHome.length,
        },
        timestamp: new Date().toISOString(),
        correlationId,
      };

      if (typeof ack === 'function') ack({ success: true, data: response, correlationId, timestamp: new Date().toISOString() });
      else socket.emit('friends:presence', response);
    } catch (err) {
      const appErr = err instanceof AppError ? err : new AppError({ code: ErrorCodes.INTERNAL_ERROR, correlationId, developerMessage: (err as Error).message });
      if (typeof ack === 'function') ack({ success: false, error: appErr.toJSON() });
    }
  });
}
