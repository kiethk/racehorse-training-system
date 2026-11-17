import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

interface HorseItem {
  id: number;
  name: string;
  breed?: string | null;
  currentStatus?: string;
}

function getOwnerHorsesEmptyStateConfig(
  horses: HorseItem[],
  filteredHorses: HorseItem[]
) {
  if (horses.length === 0) {
    return {
      title: 'You do not have any registered horses',
      description:
        'Once horses are accepted into the academy under your ownership, they will appear here. Track your pending applications or submit a new horse in Admissions.',
      actionType: 'LINK',
      actionHref: '/owner/admissions',
      actionLabel: 'View Admissions',
      actionIcon: 'clipboard',
      actionVariant: 'primary',
    };
  }

  if (filteredHorses.length === 0) {
    return {
      title: 'No matching horses found',
      description: 'Try adjusting your search query or status filter.',
      actionType: 'BUTTON',
      actionLabel: 'Clear filters',
      actionVariant: 'secondary',
    };
  }

  return null;
}

describe('OwnerHorsesView Empty State Specification', () => {
  it('renders View Admissions LinkButton when owner has zero horses registered', () => {
    const horses: HorseItem[] = [];
    const filteredHorses: HorseItem[] = [];

    const config = getOwnerHorsesEmptyStateConfig(horses, filteredHorses);
    assert.ok(config);
    assert.equal(config.title, 'You do not have any registered horses');
    assert.match(config.description, /Track your pending applications or submit a new horse in Admissions/);
    assert.equal(config.actionType, 'LINK');
    assert.equal(config.actionHref, '/owner/admissions');
    assert.equal(config.actionLabel, 'View Admissions');
    assert.equal(config.actionIcon, 'clipboard');
    assert.equal(config.actionVariant, 'primary');
  });

  it('renders Clear filters Button when owner has horses but active filters match zero', () => {
    const horses: HorseItem[] = [
      { id: 1, name: 'Secretariat', breed: 'Thoroughbred', currentStatus: 'ELIGIBLE' },
    ];
    const filteredHorses: HorseItem[] = [];

    const config = getOwnerHorsesEmptyStateConfig(horses, filteredHorses);
    assert.ok(config);
    assert.equal(config.title, 'No matching horses found');
    assert.equal(config.description, 'Try adjusting your search query or status filter.');
    assert.equal(config.actionType, 'BUTTON');
    assert.equal(config.actionLabel, 'Clear filters');
    assert.equal(config.actionVariant, 'secondary');
  });

  it('does not render empty state when filtered horses exist', () => {
    const horses: HorseItem[] = [
      { id: 1, name: 'Secretariat', breed: 'Thoroughbred', currentStatus: 'ELIGIBLE' },
    ];
    const filteredHorses = [...horses];

    const config = getOwnerHorsesEmptyStateConfig(horses, filteredHorses);
    assert.equal(config, null);
  });
});
