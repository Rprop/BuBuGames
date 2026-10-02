import Phaser from './lib/phaser.js';
import { BootScene } from './scenes/BootScene.js';
import { MenuScene } from './scenes/MenuScene.js';
import { LevelScene } from './scenes/LevelScene.js';
import { SaveManager } from './systems/SaveManager.js';
import { InputSystem } from './systems/InputSystem.js';
import { AudioManager } from './systems/AudioManager.js';
import { Telemetry } from './systems/Telemetry.js';
import { UIManager } from './ui/UIManager.js';
import { LEVELS,CHARACTERS } from './config/game.js';
class GameApp {
  constructor(){
    this.debug=new URLSearchParams(location.search).get('debug')==='1';this.currentScreen='boot';
    let storage;try{storage=window.localStorage;}catch{}this.save=new SaveManager(storage);this.audio=new AudioManager(this.save);this.telemetry=new Telemetry();
    this.input=new InputSystem({onUnlock:()=>this.audio.unlock(),onPause:reason=>{const s=this.levelScene();if(s)s.setPaused(reason==='manual'?!s.paused:true,reason);}});
    this.ui=new UIManager(this);this.resize=()=>{const scale=Math.min(innerWidth/960,innerHeight/540);document.getElementById('stage').style.transform=`scale(${scale})`;};
    window.addEventListener('resize',this.resize);this.resize();
    const allowPortrait=()=>{document.body.classList.add('allow-portrait');try{sessionStorage.setItem('bubu-v3-allow-portrait','1');}catch{}};
    try{if(sessionStorage.getItem('bubu-v3-allow-portrait')==='1')allowPortrait();}catch{}
    document.getElementById('rotate-fullscreen').onclick=async()=>{try{await document.documentElement.requestFullscreen?.();await screen.orientation?.lock?.('landscape');}catch{}};
    document.getElementById('rotate-continue')?.addEventListener('click',allowPortrait);
    this.game=new Phaser.Game({type:Phaser.AUTO,parent:'game',width:960,height:540,resolution:Math.min(window.devicePixelRatio||1,2),backgroundColor:'#fbf1df',
      antialias:true,pixelArt:false,roundPixels:false,banner:false,powerPreference:'low-power',
      input:{activePointers:3,keyboard:false},audio:{noAudio:true},
      render:{antialias:true,transparent:false,preserveDrawingBuffer:false},
      physics:{default:'arcade',arcade:{gravity:{x:0,y:1150},debug:false,fps:60,fixedStep:true,overlapBias:10}},
      fps:{target:60,min:20,smoothStep:true},scene:[BootScene,MenuScene,LevelScene]});
    this.game.registry.set('app',this);if(this.debug)this.installDebug();
  }
  levelScene(){return this.game?.scene?.isActive('Level')?this.game.scene.getScene('Level'):null;}
  open(screen){this.input.releaseAll();this.input.enabled=false;this.audio.resume();this.game.scene.stop('Level');this.game.scene.stop('Menu');this.game.scene.start('Menu',{screen});}
  play(id){const l=LEVELS.find(l=>l.id===id);if(!l||(!this.debug&&l.order>this.save.data.unlockedLevel))return;
    this.input.releaseAll();this.ui.closeModal();this.game.scene.stop('Menu');this.game.scene.stop('Level');this.game.scene.start('Level',{levelId:id});}
  result(level,run,result){this.game.scene.stop('Level');this.game.scene.start('Menu',{screen:'result',level,run,result});}
  installDebug(){
    const app=this;window.__BUBU_TEST__={
      get app(){return app;},get scene(){return app.levelScene();},
      go(order=1,character='bubu'){if(character in CHARACTERS)app.save.selectCharacter(character);app.play(LEVELS[order-1].id);},
      teleport(x,feet){app.levelScene()?.placePlayer(x,feet,{release:true});},
      keys(actions=[]){app.input.releaseAll();for(const code of actions)app.input.keys.add(code);},
      jump(){app.input.jumpQueued=true;app.input.keys.add('Space');},
      step(frames=1,dt=1000/60){app.game.loop.sleep();app.manualTime??=performance.now();for(let i=0;i<frames;i++){app.manualTime+=dt;if(i===frames-1)app.game.step(app.manualTime,dt);else app.game.headlessStep(app.manualTime,dt);}return this.state();},
      fastStep(frames=1,dt=1000/60){app.game.loop.sleep();app.manualTime??=performance.now();for(let i=0;i<frames;i++){app.manualTime+=dt;app.game.headlessStep(app.manualTime,dt);}return this.state();},
      live(){app.game.loop.wake();},
      state(){const s=app.levelScene();return !s?{screen:app.currentScreen}: {screen:'game',level:s.level.id,character:s.character.id,x:s.actor.body.center.x,y:s.actor.body.bottom,vx:s.actor.body.velocity.x,vy:s.actor.body.velocity.y,grounded:!!(s.actor.body.touching.down||s.actor.body.blocked.down),surface:s.landingSurface?.meta?.id,hearts:s.damage.hearts,damage:s.damage.hits,combo:s.combo.count,maxCombo:s.combo.max,perfect:s.perfect.count,happiness:s.happiness,photos:s.save.data.levels[s.level.id].photos,elapsed:s.simTime,paused:s.paused,challenge:s.challenge.active?{id:s.challenge.active.def.challengeId,progress:s.challenge.progress}:null};}
    };
  }
}
new GameApp();
