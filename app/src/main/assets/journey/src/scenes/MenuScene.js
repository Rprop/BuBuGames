import Phaser from '../lib/phaser.js';
import { addBackdrop } from '../art/Artwork.js';
export class MenuScene extends Phaser.Scene {
  constructor(){super('Menu');}
  create(data={}){
    this.app=this.game.registry.get('app');this.app.input.enabled=false;this.app.input.releaseAll();
    const screen=data.screen||'title',theme=data.level?.theme||(screen==='album'?'starlight':'commute');
    this.backdrop=addBackdrop(this,theme);this.cameras.main.setScroll(300,130);this.backdrop.update(this.cameras.main);
    if(screen==='result')this.app.ui.result(data.level,data.run,data.result);else this.app.ui[screen]();
    this.app.currentScreen=screen;
  }
}
