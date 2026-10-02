// Pin the exact Phaser 3.90.0 distribution shipped in the supplied project.
// Only this third-party engine stays minified; all game logic is readable source.
import { r as requirePhaser, g as interop } from '../../vendor/phaser.js';
const Phaser = interop(requirePhaser());
export default Phaser;
