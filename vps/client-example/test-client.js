/**
 * Simple Node.js test client for FOMO Signaling Server
 * Usage: node test-client.js
 * Requires: npm i socket.io-client jsonwebtoken
 */
import { io } from 'socket.io-client';
import jwt from 'jsonwebtoken';

const SIGNALING_URL = process.env.SIGNALING_URL || 'http://localhost:3000';
const JWT_SECRET = process.env.JWT_SECRET || 'dev_secret_change_in_production_fomo_2026';

function createTestToken(userId, displayName) {
  return jwt.sign(
    { userId, displayName, email: `${userId}@test.com` },
    JWT_SECRET,
    { expiresIn: '1h' }
  );
}

async function run() {
  const userId = `test_${Math.random().toString(36).slice(2, 7)}`;
  const token = createTestToken(userId, `Tester ${userId}`);

  console.log(`Connecting as ${userId} to ${SIGNALING_URL} ...`);

  const socket = io(SIGNALING_URL, {
    auth: { token, displayName: `Tester ${userId}` },
    transports: ['websocket'],
  });

  socket.on('connect', () => {
    console.log(`✅ Connected socketId=${socket.id} userId=${userId}`);

    // Test presence
    socket.emit('presence:get_online', { limit: 10 }, (res) => {
      console.log('Online users:', JSON.stringify(res, null, 2));
    });

    // Test chat join
    socket.emit('chat:join', { chatId: 'test_chat_123' }, (res) => {
      console.log('chat:join response', res);
      socket.emit('chat:message', {
        chatId: 'test_chat_123',
        message: { text: 'Hello from test client!', type: 'TEXT' },
      });
    });

    // Test map join
    socket.emit('map:join', { city: 'Johannesburg' }, (res) => {
      console.log('map:join', res);
    });

    // Test lobby join
    socket.emit('lobby:join', { venueId: 'fomo_club' }, (res) => {
      console.log('lobby:join', res);
      setTimeout(() => {
        socket.emit('lobby:message', { venueId: 'fomo_club', text: 'Hello lobby from test!' });
      }, 1000);
    });

    // Test live list
    socket.emit('live:list', {}, (res) => {
      console.log('live:list', res);
    });
  });

  socket.on('connected', (data) => console.log('Server connected ack:', data));
  socket.on('chat:new_message', (msg) => console.log('→ chat:new_message', msg));
  socket.on('map:presence', (data) => console.log('→ map:presence', data));
  socket.on('lobby:new_message', (data) => console.log('→ lobby:new_message', data));
  socket.on('user:online', (data) => console.log('→ user:online', data));
  socket.on('call:incoming', (data) => console.log('📞 Incoming call', data));
  socket.on('live:new', (data) => console.log('🔴 New live', data));
  socket.on('error', (err) => console.error('❌ error event', err));
  socket.on('connect_error', (err) => console.error('❌ connect_error', err.message));
  socket.on('disconnect', (reason) => console.log('Disconnected', reason));
}

run().catch(console.error);
