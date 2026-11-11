/**
 * 2D Horse Anatomy Blueprint — source of truth for the 8 injury regions.
 *
 * User-approved approach: region bands are defined on a 2D anatomical frame
 * (side-view silhouette, normalized anatomy coordinates n) FIRST, then the 3D
 * click classifier consumes the same bands. Every number below is anchored to a
 * live-verified click measurement (2026-11-10 session, urgent-horse.glb
 * skinned runtime bounds), not to a guess.
 *
 * Anatomy frame (see normalizeScenePoint in injuryZones.ts):
 *   n.x: 0 = Left side, 0.5 = Spine midline, 1 = Right side (+X is Left)
 *   n.y: 0 = Hooves, 1 = Top of head/ears
 *   n.z: 0 = Nose, 1 = Tail (+Z is Front)
 *
 * Measured anchors (renderer canvas CSS px, viewport 762x418 at the session
 * camera). Click region -> normalized anatomy -> approved label:
 *   forehead           n.y 0.944                         -> Head
 *   muzzle bridge      n.y 0.860                         -> Head
 *   upper neck/crest   n.y 0.810, n.z 0.363              -> Neck   (was Head)
 *   saddle top/withers n.y 0.728, n.z 0.650              -> Back   (was Neck)
 *   mid flank          n.y 0.588, n.z 0.601, fn.y -0.14  -> Abdomen
 *   rear flank/haunch  n.y 0.366, n.z 0.844              -> Abdomen
 *   underbelly         n.y 0.111, n.z 0.811              -> Abdomen (was Hindleg)
 *   chest/girth        n.y 0.478, fn.x 0.68, fn.y -0.49  -> Abdomen (was skipped by x-band legs)
 *   belly-side barrel  n.y 0.486, n.x 0.222, fn.x 0.997  -> Abdomen (was Foreleg)
 *
 * Leg cylinder faces (measured on the near foreleg shaft + hoof at ground):
 *   shaft              n.x 0.62/0.38 sides, n.y 0.068..0.298, fn.x ±0.98..±1.00, n.z 0.40
 *   hoof (front face)  n.y 0.068, fn.z 0.83, fn.y 0.52
 * => legs share the belly x-range; the face normal (pure lateral fn.x or
 *    forward hoof face) plus height is the discriminator, NOT the x-band.
 *
 * Open boundary (explicit): the HIND legs render visible at screen pixels
 * where the raycast (static pose geometry) does not hit — the skinned render
 * and the raycastable mesh are displaced there; every hind leg probe below
 * n.y 0.26 returned NO-HIT. Until a skinning-aware raycast exists, hind texts
 * may need direct clicks on the thigh area (n.y 0.26..0.36, n.z 0.75..0.9)
 * which classify as Abdomen; refine when the renderer exposes leg faces.
 */
export const HORSE_ANATOMY_BLUEPRINT = {
  /** Head: only the head silhouette above the neck crest (trán 0.944, mõm 0.860). */
  headYMin: 0.86,
  /** Neck sits above the shoulder/withers line and in the forward Z band. */
  neckYMin: 0.66,
  /** Neck occupies only the forward z-range ahead of the withers (cổ n.z 0.363). */
  neckZMax: 0.46,
  /** Upper back (withers, saddle, loin, croup) is always Back above this height. */
  backYMin: 0.62,
  /** Transition band between upper back and mid-flank belly (0.55 < n.y <= 0.62). */
  trunkYMin: 0.55,
  /** Spine-facing surface normal threshold in the 0.55..0.62 transition band. */
  spineNormalY: 0.15,
  /** Abdomen longitudinal bounds: central barrel between front shoulder and rear haunch. */
  abdomenZMin: 0.42,
  abdomenZMax: 0.72,
  /** Front lower shoulder/forearm below this height (when n.z < abdomenZMin) belongs to Foreleg. */
  frontShoulderYMax: 0.52,
  /** All 4 legs sit below this height at every camera/normal angle. */
  legYMax: 0.44,
  /** Foreleg occupies the forward half of the leg band (approved silhouette split). */
  forelegZMax: 0.55,
} as const;

/**
 * Geometry-zone classifier on the 2D blueprint frame. Returns the legacy six
 * zone ids plus the back/abdomen region keys so the downstream 8-region
 * resolver keeps one entry point.
 */
export function zoneFromBlueprint(
  n: { x: number; y: number; z: number },
  surfaceNormal?: { x: number; y: number; z: number } | null,
): string {
  const b = HORSE_ANATOMY_BLUEPRINT;

  if (n.y > b.headYMin) return 'head';

  // Confined Neck region: high above shoulder (n.y > 0.66) and ahead of withers (n.z < 0.46).
  if (n.y > b.neckYMin && n.z < b.neckZMax) return 'neck';

  // Upper trunk above backYMin (0.62) outside the neck is always Back (withers, saddle, croup).
  if (n.y > b.backYMin) return 'back';

  // All 4 legs below legYMax (0.44) classify as legs at every click angle.
  if (n.y < b.legYMax) {
    return n.z < b.forelegZMax ? 'foreleg' : 'hindleg';
  }

  // Rear thigh / haunch / lower hindquarters (n.z > 0.72, n.y <= 0.62) belong to Hindleg.
  if (n.z > b.abdomenZMax) {
    return 'hindleg';
  }

  // Front lower shoulder / forearm (n.z < 0.42, n.y < 0.52) belongs to Foreleg.
  if (n.z < b.abdomenZMin && n.y < b.frontShoulderYMax) {
    return 'foreleg';
  }

  // Mid-upper trunk transition band (0.55 < n.y <= 0.62): upward-facing surfaces are Back,
  // lateral/downward-facing surfaces (like mid flank n.y 0.588, fn.y -0.14) are Abdomen.
  if (n.y > b.trunkYMin) {
    if (surfaceNormal) {
      const len = Math.hypot(surfaceNormal.x, surfaceNormal.y, surfaceNormal.z) || 1;
      return surfaceNormal.y / len > b.spineNormalY ? 'back' : 'abdomen';
    }
    return 'back';
  }

  // Remaining central barrel (0.42 <= n.z <= 0.72, 0.44 <= n.y <= 0.55) and upper chest.
  return 'abdomen';
}
