import assert from 'node:assert/strict';
import { test } from 'node:test';
import { entityHref } from '../entityRoute.ts';

test('entity routes respect role and required context', () => {
  assert.equal(entityHref('horse', 7, 'CLUB_MANAGER'), '/manager/horses/7');
  assert.equal(entityHref('horse', 7, 'HORSE_OWNER'), '/owner/horses/7');
  assert.equal(entityHref('horse', 7, 'HEAD_TRAINER'), '/trainer/horses/7');
  assert.equal(entityHref('horse', 7, 'VETERINARIAN'), '/veterinarian/records?horseId=7');
  assert.equal(entityHref('horse', 7, 'GROOM'), null);
  assert.equal(entityHref('horse', 7, 'GROOM', { stallCode: 'A 1' }), '/groom/stable?stall=A%201');
  assert.equal(entityHref('owner', 4, 'CLUB_MANAGER'), '/manager/accounts?userId=4');
  assert.equal(entityHref('user', 4, 'GROOM'), null);
  assert.equal(entityHref('admission', 2, 'VETERINARIAN'), '/veterinarian/admissions');
  assert.equal(entityHref('admission', 2, 'GROOM'), '/groom/admissions/2');
  assert.equal(entityHref('admission', 2, 'HORSE_OWNER'), '/owner/admissions/2');
  assert.equal(entityHref('admission', 2, 'HEAD_TRAINER'), '/trainer/admissions/2');
  assert.equal(entityHref('admission', 2, 'CLUB_MANAGER'), '/manager/admissions/2');
  assert.equal(entityHref('race', 3, 'HEAD_TRAINER'), '/trainer/racing/3');
  assert.equal(entityHref('race', 3, 'CLUB_MANAGER'), null);
  assert.equal(entityHref('plan', 3, 'HEAD_TRAINER'), '/trainer/plans/3');
  assert.equal(entityHref('stall', 'B 2', 'CLUB_MANAGER'), '/manager/stable?stall=B%202');
  assert.equal(entityHref('stall', 'B 2', 'HEAD_TRAINER'), '/trainer/stable?stall=B%202');
  assert.equal(entityHref('stall', 'B 2', 'GROOM'), '/groom/stable?stall=B%202');
  assert.equal(entityHref('stall', 'B 2', 'HORSE_OWNER'), null);
  assert.equal(entityHref('editRequest', 5, 'GROOM'), '/groom/horse-edit-requests/5');
  assert.equal(entityHref('editRequest', 5, 'HORSE_OWNER', { horseId: 9 }), '/owner/horses/9');
  assert.equal(entityHref('editRequest', 5, 'HORSE_OWNER'), null);
});
