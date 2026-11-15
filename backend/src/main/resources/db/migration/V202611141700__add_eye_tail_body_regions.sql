-- Eye and tail were previously folded into HEAD / BACK when the 3D model classified clicks.
-- They are now first-class regions: the model map assigns them per triangle (see
-- frontend/scripts/build-injury-regions.mjs) and the vet can pick them in the marker selector.
INSERT INTO horse_body_regions (code, display_name, model_node_key, is_active) VALUES
    ('EYE', 'Eye', 'eye', true),
    ('TAIL', 'Tail', 'tail', true)
ON CONFLICT (code) DO NOTHING;
