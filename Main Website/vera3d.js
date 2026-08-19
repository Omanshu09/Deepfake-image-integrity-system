(function () {
    const container = document.getElementById('vera-canvas-container');
    if (!container || typeof THREE === 'undefined') return;

    const W = 120, H = 160;

    // ── Renderer ───────────────────────────────────────────────
    const renderer = new THREE.WebGLRenderer({ antialias: true, alpha: true });
    renderer.setSize(W, H);
    renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
    renderer.setClearColor(0x000000, 0);
    container.appendChild(renderer.domElement);
    renderer.domElement.style.pointerEvents = 'none';

    // ── Scene ──────────────────────────────────────────────────
    const scene = new THREE.Scene();

    // Camera looks straight at the face — centred on the robot's face
    const camera = new THREE.PerspectiveCamera(38, W / H, 0.1, 1000);
    camera.position.set(0, 0, 9);
    camera.lookAt(0, 0, 0);

    // ── Lighting ───────────────────────────────────────────────
    scene.add(new THREE.AmbientLight(0xffffff, 0.85));

    const keyLight = new THREE.DirectionalLight(0xffffff, 1.0);
    keyLight.position.set(3, 5, 8);
    scene.add(keyLight);

    const rimLight = new THREE.DirectionalLight(0xccccff, 0.5);
    rimLight.position.set(-4, -2, 6);
    scene.add(rimLight);

    // Strong front fill so face is always bright
    const frontLight = new THREE.PointLight(0xffffff, 0.8, 30);
    frontLight.position.set(0, 0, 8);
    scene.add(frontLight);

    // ── Materials ──────────────────────────────────────────────
    const white = new THREE.MeshStandardMaterial({ color: 0xf0eeff, roughness: 0.2, metalness: 0.05 });
    const silver = new THREE.MeshStandardMaterial({ color: 0x888899, roughness: 0.4, metalness: 0.7 });
    const visor = new THREE.MeshStandardMaterial({ color: 0x221a3f, roughness: 0.05, metalness: 0.3 }); // Lighter dark-blueish purple

    // Cloneable eye material — bright emissive pink/purple
    function makeEyeMat() {
        return new THREE.MeshStandardMaterial({
            color: 0xffe6f5, // pinky type
            emissive: new THREE.Color(0xff88ff),
            emissiveIntensity: 1.5, // softer glow
            roughness: 0.0,
            metalness: 0.0,
            transparent: false,
            depthWrite: true
        });
    }

    const smileMat = new THREE.MeshStandardMaterial({
        color: 0xffe6f5,
        emissive: new THREE.Color(0xff88ff),
        emissiveIntensity: 1.5,
        roughness: 0.1
    });

    const chestMat = new THREE.MeshStandardMaterial({
        color: 0xcc77ff,
        emissive: new THREE.Color(0xaa00ee),
        emissiveIntensity: 2.2,
        roughness: 0.05
    });

    // ── Robot ──────────────────────────────────────────────────
    const robot = new THREE.Group();

    // ── HEAD ── centred at y = 1.1
    const headGroup = new THREE.Group();
    headGroup.position.set(0, 1.1, 0);

    // Outer head shell — oblate sphere
    const headShell = new THREE.Mesh(
        new THREE.SphereGeometry(1.15, 64, 64),
        white
    );
    headShell.scale.set(1.1, 0.92, 1.0);
    headGroup.add(headShell);

    // Visor face — flattened dark sphere
    const visorMesh = new THREE.Mesh(
        new THREE.SphereGeometry(1.0, 48, 48),
        visor
    );
    visorMesh.scale.set(0.95, 0.70, 0.50);  // front z = 1.0 * 0.5 = 0.5
    visorMesh.position.set(0, 0, 0.75);      // front tip at 0.75 + 0.5 = 1.25 (Head surface is at 1.15)
    headGroup.add(visorMesh);

    // ── EYES — positioned clearly in FRONT of visor (z > 1.25) ──
    const leftEyeMat  = makeEyeMat();
    const rightEyeMat = makeEyeMat();

    // Rounder eyes, slightly wider apart
    const leftEye = new THREE.Mesh(new THREE.SphereGeometry(0.14, 32, 32), leftEyeMat);
    leftEye.scale.set(1.0, 1.0, 0.4); // Perfectly round from the front
    leftEye.position.set(-0.38, 0.05, 1.30);   
    headGroup.add(leftEye);

    const rightEye = new THREE.Mesh(new THREE.SphereGeometry(0.14, 32, 32), rightEyeMat);
    rightEye.scale.set(1.0, 1.0, 0.4);
    rightEye.position.set(0.38, 0.05, 1.30);
    headGroup.add(rightEye);

    // ── SMILE ── filled half-circle
    const smileMesh = new THREE.Mesh(
        new THREE.CircleGeometry(0.13, 32, 0, Math.PI),
        smileMat
    );
    smileMesh.rotation.z = Math.PI;   // open side up = smile
    smileMesh.position.set(0, -0.16, 1.29);
    headGroup.add(smileMesh);

    robot.add(headGroup);

    // ── NECK ──
    const neck = new THREE.Mesh(new THREE.CylinderGeometry(0.35, 0.40, 0.22, 32), silver);
    neck.position.set(0, 0.28, 0);
    robot.add(neck);

    // ── BODY ── egg sphere centred at y = -0.65
    const body = new THREE.Mesh(new THREE.SphereGeometry(0.95, 64, 64), white);
    body.scale.set(0.85, 1.22, 0.85);
    body.position.set(0, -0.65, 0);
    robot.add(body);

    // Chest glow
    const chestDot = new THREE.Mesh(new THREE.SphereGeometry(0.16, 32, 32), chestMat);
    chestDot.position.set(0, -0.3, 0.74);
    robot.add(chestDot);

    // ── ARMS ── wing paddles with ball joints
    function makeArm(side) {   // side: -1 = left, +1 = right
        const grp = new THREE.Group();

        const joint = new THREE.Mesh(new THREE.SphereGeometry(0.18, 16, 16), silver);
        grp.add(joint);

        const paddle = new THREE.Mesh(new THREE.SphereGeometry(0.20, 32, 32), white);
        paddle.scale.set(0.85, 2.3, 0.6);
        paddle.position.set(0, -0.42, 0);
        grp.add(paddle);

        grp.position.set(side * 0.94, -0.18, 0);
        grp.rotation.z = side * -0.22;   // left tilts right, right tilts left
        return grp;
    }

    const leftArm  = makeArm(-1);
    const rightArm = makeArm( 1);
    robot.add(leftArm, rightArm);

    robot.position.set(0, 0, 0);
    scene.add(robot);

    // ── State Machine ──────────────────────────────────────────
    let state = 'idle';
    let isHovered = false;

    window.setVeraState = function (s) {
        // Don't override wave state from outside unless explicitly
        if (isHovered && s === 'idle') return;
        state = s || 'idle';
    };

    // Hover → wave
    const mascot = document.getElementById('robot-mascot');
    if (mascot) {
        mascot.addEventListener('mouseenter', () => {
            isHovered = true;
            state = 'wave';
        });
        mascot.addEventListener('mouseleave', () => {
            isHovered = false;
            // only go back to idle if not in thinking/happy
            if (state === 'wave') state = 'idle';
        });
    }

    // ── Animation ─────────────────────────────────────────────
    const clock = new THREE.Clock();
    const lerp  = (a, b, t) => a + (b - a) * t;

    function animate() {
        requestAnimationFrame(animate);
        const t = clock.getElapsedTime();

        // ── IDLE ──────────────────────────────────────────────
        if (state === 'idle') {
            // Gentle float
            robot.position.y = Math.sin(t * 1.8) * 0.15;
            robot.rotation.y = Math.sin(t * 0.5) * 0.05;

            // Arms lazy flap
            leftArm.rotation.z  = lerp(leftArm.rotation.z,  -0.22 + Math.sin(t * 1.5) * 0.10, 0.1);
            rightArm.rotation.z = lerp(rightArm.rotation.z,  0.22 + Math.sin(t * 1.5 + 1.0) * 0.10, 0.1);

            // Eye glow pulse
            leftEyeMat.emissiveIntensity  = 2.8 + Math.sin(t * 2.3) * 0.7;
            rightEyeMat.emissiveIntensity = 2.8 + Math.sin(t * 2.3 + 0.3) * 0.7;

            // Chest pulse
            chestMat.emissiveIntensity = 1.6 + Math.sin(t * 2.8) * 0.6;

            // Reset scales
            leftEye.scale.set(1.0, 1.0, 0.4);
            rightEye.scale.set(1.0, 1.0, 0.4);
            smileMesh.scale.set(1.0, 1.0, 1.0);
            smileMat.emissiveIntensity = 1.5;

        // ── WAVE (hover) ────────────────────────────────────────
        } else if (state === 'wave') {
            robot.position.y = Math.sin(t * 2.0) * 0.15;

            // Whole robot does a friendly tilt
            robot.rotation.z = Math.sin(t * 4.0) * 0.10;

            // Right arm waves energetically
            rightArm.rotation.z = lerp(rightArm.rotation.z, 1.2 + Math.sin(t * 8.0) * 0.45, 0.12);
            leftArm.rotation.z  = lerp(leftArm.rotation.z,  -0.22 + Math.sin(t * 1.5) * 0.08, 0.1);

            // Eyes bright and big
            leftEyeMat.emissiveIntensity  = 4.0;
            rightEyeMat.emissiveIntensity = 4.0;
            leftEye.scale.set(1.1, 1.1, 0.4);
            rightEye.scale.set(1.1, 1.1, 0.4);

            smileMesh.scale.set(1.3, 1.0, 1.0);
            smileMat.emissiveIntensity = 2.5;
            chestMat.emissiveIntensity = 2.5;

        // ── THINKING ───────────────────────────────────────────
        } else if (state === 'thinking') {
            robot.position.y = Math.sin(t * 1.8) * 0.15;
            robot.rotation.z = Math.sin(t * 3.0) * 0.12;   // head-tilt confusion
            robot.rotation.y = 0;

            // Eyes squint
            const sq = 0.3 + Math.abs(Math.sin(t * 4.5)) * 0.4;
            leftEye.scale.set(1.0, sq, 0.4);
            rightEye.scale.set(1.0, sq, 0.4);
            leftEyeMat.emissiveIntensity  = 1.5 + Math.sin(t * 9) * 0.7;
            rightEyeMat.emissiveIntensity = 1.5 + Math.sin(t * 9 + 0.4) * 0.7;

            // Smaller mouth = uncertain
            smileMesh.scale.set(0.5, 1.0, 1.0);
            smileMat.emissiveIntensity = 1.0;

            // Left arm raises (thinking pose)
            leftArm.rotation.z  = lerp(leftArm.rotation.z,  -1.1 + Math.sin(t * 2) * 0.1, 0.07);
            rightArm.rotation.z = lerp(rightArm.rotation.z,  0.22, 0.05);

            chestMat.emissiveIntensity = 0.8 + Math.abs(Math.sin(t * 7)) * 1.5;

        // ── HAPPY ──────────────────────────────────────────────
        } else if (state === 'happy') {
            robot.position.y = Math.abs(Math.sin(t * 8.0)) * 0.30;
            robot.rotation.z = 0;
            robot.rotation.y = 0;

            // Eyes wide + super bright
            leftEye.scale.set(1.2, 1.2, 0.4);
            rightEye.scale.set(1.2, 1.2, 0.4);
            leftEyeMat.emissiveIntensity  = 5.0;
            rightEyeMat.emissiveIntensity = 5.0;

            // Big grin
            smileMesh.scale.set(1.5, 1.0, 1.0);
            smileMat.emissiveIntensity = 3.0;

            // Right arm fast wave
            rightArm.rotation.z = lerp(rightArm.rotation.z, 1.2 + Math.sin(t * 12) * 0.55, 0.15);
            leftArm.rotation.z  = lerp(leftArm.rotation.z, -0.22, 0.1);

            chestMat.emissiveIntensity = 3.5;
        }

        renderer.render(scene, camera);
    }

    animate();
})();
