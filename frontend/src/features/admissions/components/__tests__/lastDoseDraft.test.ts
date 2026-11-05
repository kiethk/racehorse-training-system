import { describe, it } from 'node:test';
import assert from 'node:assert/strict';
import { lastDoseDraft, toVaccinationRequests, validateVaccinationDrafts } from '../../../care/components/preventive/vaccinationDrafts.ts';

const TODAY = '2026-10-26';
const influenza = { id: 2, defaultDosage: '1 mL', defaultRoute: 'IM' };

describe('Admission last dose: passport date or a dose given at the club today', () => {
  it('a past date is a passport (historical) date without dosage', () => {
    const draft = lastDoseDraft(influenza, '2026-04-01', TODAY, false);
    assert.deepEqual(draft, { key: 'last-dose-2', protocolId: 2, dosage: '', route: '', source: 'HISTORICAL', administeredOn: '2026-04-01' });
  });

  it('today without the club box stays a passport date (the default)', () => {
    assert.equal(lastDoseDraft(influenza, TODAY, TODAY, false)?.source, 'HISTORICAL');
  });

  it('today with the club box becomes a dose given here, prefilled from the protocol', () => {
    const draft = lastDoseDraft(influenza, TODAY, TODAY, true);
    assert.equal(draft?.source, 'ADMINISTERED');
    assert.equal(draft?.dosage, '1 mL');
    assert.equal(draft?.route, 'IM');
    assert.equal(draft?.administeredOn, TODAY);
  });

  it('keeps what the vet typed while the dose stays at the club', () => {
    const typed = { key: 'last-dose-2', protocolId: 2, dosage: '2 mL', route: 'IV', source: 'ADMINISTERED' as const, administeredOn: TODAY };
    const draft = lastDoseDraft(influenza, TODAY, TODAY, true, typed);
    assert.equal(draft?.dosage, '2 mL');
    assert.equal(draft?.route, 'IV');
  });

  it('a dose at the club can only be today: another date falls back to historical', () => {
    assert.equal(lastDoseDraft(influenza, '2026-10-20', TODAY, true)?.source, 'HISTORICAL');
  });

  it('unticking the club box goes back to a historical date', () => {
    const atClub = lastDoseDraft(influenza, TODAY, TODAY, true)!;
    const back = lastDoseDraft(influenza, TODAY, TODAY, false, atClub);
    assert.equal(back?.source, 'HISTORICAL');
    assert.equal(back?.dosage, '');
  });

  it('an empty date removes the row', () => {
    assert.equal(lastDoseDraft(influenza, '', TODAY, true), null);
  });

  it('a dose at the club needs a dosage and is sent without a date (the server uses today)', () => {
    const draft = { ...lastDoseDraft(influenza, TODAY, TODAY, true)!, dosage: '  ' };
    assert.deepEqual(validateVaccinationDrafts([draft], TODAY), { 'last-dose-2': 'Dosage is required.' });
    const [request] = toVaccinationRequests([{ ...draft, dosage: '1 mL' }]);
    assert.deepEqual(request, { protocolId: 2, dosage: '1 mL', route: 'IM', source: 'ADMINISTERED', administeredOn: undefined });
  });
});
