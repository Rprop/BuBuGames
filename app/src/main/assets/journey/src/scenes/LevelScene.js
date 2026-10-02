import Phaser from '../lib/phaser.js';
import { LEVEL_BY_ID,CHARACTERS,PHOTO_BY_ID,TUNING } from '../config/game.js';
import { validateLevel,objects,props } from '../tiled/LevelValidator.js';
import { TiledObjectFactory } from '../tiled/TiledObjectFactory.js';
import { addBackdrop,PALETTE } from '../art/Artwork.js';
import { ComboSystem } from '../systems/ComboSystem.js';
import { MovementSystem } from '../systems/MovementSystem.js';
import { PerfectSystem } from '../systems/PerfectSystem.js';
import { DamageSystem } from '../systems/DamageSystem.js';
import { ChallengeManager } from '../systems/ChallengeManager.js';
import { Effects } from '../systems/Effects.js';
const overlaps=(b,r)=>b.right>r.x&&b.left<r.x+r.width&&b.bottom>r.y&&b.top<r.y+r.height;
const circleHits=(b,x,y,r)=>{const dx=x-Math.max(b.left,Math.min(x,b.right)),dy=y-Math.max(b.top,Math.min(y,b.bottom));return dx*dx+dy*dy<r*r;};
export class LevelScene extends Phaser.Scene {
  constructor(){super('Level');}
  create({levelId}={}){
    this.app=this.game.registry.get('app');this.save=this.app.save;this.audio=this.app.audio;this.ui=this.app.ui;this.telemetry=this.app.telemetry;
    this.level=LEVEL_BY_ID[levelId]||Object.values(LEVEL_BY_ID)[0];this.character=CHARACTERS[this.save.data.selectedCharacter];
    this.map=this.cache.json.get(this.level.id);const validation=validateLevel(this.map,this.level);
    if(!validation.ok){throw new Error('关卡校验失败：'+validation.errors.join('; '));}
    this.simTime=0;this.finished=false;this.finishElapsed=0;this.paused=false;this.happiness=0;this.runPhotos=new Set();this.landingSurface=null;this.airTouched=new Set();this.bounceCredited=new Set();this.photoFlash=0;this.trailClock=0;this.storyModule=-1;
    this.app.currentScreen='game';this.app.input.releaseAll();this.app.input.enabled=true;this.audio.resume();
    this.telemetry.clock=()=>this.simTime;this.telemetry.emit('level_start',{levelId:this.level.id,character:this.character.id});
    this.backdrop=addBackdrop(this,this.level.theme);
    const spawn=objects(this.map,'spawn')[0],sp=props(spawn);this.respawnPoint={x:spawn.x,y:spawn.y,facing:sp.facing||1,order:0};
    this.physics.world.setBounds(0,0,this.map.width*48,980,true,true,true,false);
    this.actor=this.physics.add.sprite(spawn.x,spawn.y-27,'actor-hitbox').setAlpha(0);
    this.actor.setCollideWorldBounds(true).setMaxVelocity(650,900);this.actor.body.setSize(28,54);
    this.shadow=this.add.ellipse(spawn.x,spawn.y,36,9,0x4d4939,.14).setDepth(4);
    this.avatar=this.add.sprite(spawn.x,spawn.y+2,`${this.character.id}-idle`).setOrigin(.5,1).setDisplaySize(82,82).setDepth(20);
    this.effects=new Effects(this);
    this.combo=new ComboSystem(this.character.comboMs,{onAdd:(n,reason)=>{if([5,10,15,20].includes(n)){this.audio.play('milestone');this.effects.floatText(this.actor.x,this.actor.body.top-20,n>=20?'幸福心流':`幸福连击 ×${n}`,0xc39759,20);}},onBreak:(reason,n)=>this.telemetry.emit('combo_break',{reason,combo:n,x:Math.round(this.actor.x)})});
    this.movement=new MovementSystem(this,this.actor,this.character,this.app.input);this.perfect=new PerfectSystem(this,this.character);this.damage=new DamageSystem(this);
    new TiledObjectFactory(this,this.map).build();this.level={...this.level,nextLevel:this.goalConfig.nextLevel||null};this.challenge=new ChallengeManager(this,this.map);
    this.collider=this.physics.add.collider(this.actor,this.surfaces,this.platformCollision,this.processPlatform,this);
    const camera=this.cameras.main,cb=objects(this.map,'cameraBounds')[0],cp=props(cb);
    camera.setBounds(cb.x,cb.y,cb.width,cb.height);camera.startFollow(this.actor,false,cp.lerpX,cp.lerpY);camera.setFollowOffset(-cp.lookAhead,70);
    camera.setScroll(0,Math.max(0,Math.min(228,spawn.y-367)));this.backdrop.update(camera);
    this.drawGoal();this.ui.hud(this);this.toast(this.character.id==='bubu'?'← → 移动，空格跳跃。落在金色中心线，会有小惊喜。':'← → 移动，空格跳跃。在空中松开再按，还能多跳一次。',4200);
    // DOM controls already release each pointer independently. Do not bind Phaser's
    // pointerupoutside to releaseAll(): on real multi-touch a jump finger can otherwise
    // cancel the still-held left/right finger.
    this.events.once('shutdown',()=>{this.app.input.enabled=false;this.app.input.releaseAll();this.effects.destroy();this.challenge.cancel();this.ui.closeModal();this.telemetry.clock=()=>0;});
  }
  drawGoal(){
    const x=this.goalObject.x+65,y=this.goalObject.y+this.goalObject.height,night=this.level.theme==='starlight';
    const g=this.add.graphics().setDepth(1);
    g.lineStyle(2,night?0xe6d4ad:0xb6a389,.7);g.beginPath();g.moveTo(x-260,y-173);g.lineTo(x-40,y-110);g.lineTo(x+100,y-167);g.strokePath();
    for(let i=0;i<8;i++){const xx=x-250+i*44,yy=y-162+Math.sin(i/7*Math.PI)*47;g.fillStyle(0xffe1a1,.75);g.fillCircle(xx,yy,4);}
    if(night){g.fillStyle(0xcbba94,1);g.fillTriangle(x-185,y,x-105,y-115,x-25,y);g.fillStyle(0xf4ddb1,1);g.fillTriangle(x-130,y,x-105,y-89,x-69,y);}
    else{g.fillStyle(night?0xc5b088:0xd9be9b,.95);g.fillRoundedRect(x-180,y-102,136,102,12);g.fillStyle(0xacb696,.95);g.fillTriangle(x-195,y-99,x-112,y-160,x-31,y-99);g.fillStyle(0xffe6b2,1);g.fillRoundedRect(x-144,y-58,35,44,5);}
    this.otherAvatar=this.add.sprite(x+7,y+2,`${this.character.id==='bubu'?'yier':'bubu'}-idle`).setOrigin(.5,1).setDisplaySize(82,82).setFlipX(true).setDepth(20);
    this.add.text(x-115,y-205,night?'灯光就在前面':'有人在等你',{fontFamily:'Microsoft YaHei',fontSize:'17px',color:night?'#eddfbe':'#a0896c'}).setOrigin(.5).setDepth(6);
  }
  processPlatform(player,surface){
    if(!surface.body.enable)return false;
    if(!surface.meta?.oneWay)return true;
    const a=player.body,b=surface.body,prevBottom=a.prev.y+a.height,prevTop=b.prev?.y??b.top;
    const relativeVy=a.velocity.y-(b.velocity?.y||0),tolerance=7+Math.abs(b.velocity?.y||0)/60;
    return relativeVy>=-2 && prevBottom<=Math.max(prevTop,b.top)+tolerance;
  }
  platformCollision(player,surface){if(player.body.touching.down||player.body.blocked.down)this.landingSurface=surface;}
  onLanding(surface,vy,flight){
    if(surface?.meta?.springId){const spring=this.springs.find(x=>x.id===surface.meta.springId);if(spring&&this.simTime-spring.lastAt>420){this.bounce(spring);return true;}}
    this.airTouched.clear();const perfect=this.perfect.landing(surface,vy,flight);this.challenge.onLanding(surface,perfect);return false;
  }
  bounce(spring){
    if(this.airTouched.has(spring.id)&&spring.balloon)return;
    spring.lastAt=this.simTime;this.airTouched.add(spring.id);
    this.movement.launch(spring.impulse??-710,spring.impulseX??null,spring.boostMs??0);
    if(!this.bounceCredited.has(spring.id)){this.combo.add(1,'bounce');this.bounceCredited.add(spring.id);}
    this.challenge.emit('bounce',spring.id);this.audio.play('spring');this.effects.burst(spring.x,spring.y,0xd6c49b,10);this.shake(.001,75);
  }
  modifiersAtPlayer(){
    const effect={moveMultiplier:1,airControlMultiplier:1,extraGravityY:0,conveyor:0,slippery:false};
    for(const m of this.modifiers){if(!overlaps(this.actor.body,m.rect))continue;
      if(m.modifierKind==='wind'){effect.airControlMultiplier=m.airControlMultiplier??1;effect.extraGravityY+=m.extraGravityY??0;}
      else if(m.modifierKind==='slow')effect.moveMultiplier*=m.moveMultiplier??1;
      else if(m.modifierKind==='conveyor')effect.conveyor+=(m.speed??0)*(m.direction??1);
      else if(m.modifierKind==='slippery'){effect.slippery=true;effect.moveMultiplier*=m.moveMultiplier??1;}
      else if(m.modifierKind==='bounce'&&this.simTime-m.lastAt>700)effect.bounce=m;
    }
    return effect;
  }
  collectAndSwitch(dt){
    const b=this.actor.body;
    for(const t of this.tokens){if(t.collected)continue;t.sp.y=t.y+Math.sin(this.simTime*.003+t.x*.01)*3;
      if(circleHits(b,t.x,t.sp.y,19)){t.collected=true;t.sp.setVisible(false);this.happiness=Math.min(this.level.totalCollectibles,this.happiness+Number(t.value??1));
        this.combo.add(Number(t.value??1),'happiness');this.audio.play('collect',this.combo.count);this.effects.burst(t.x,t.y,0xefcd86,7);this.challenge.emit('collect',t.id);}}
    for(const sp of this.springs){
      const active=!sp.challengeId||this.challenge.active?.def.challengeId===sp.challengeId;sp.sp.setAlpha(active?1:.22);
      if(active&&sp.balloon&&!this.airTouched.has(sp.id)&&this.simTime-sp.lastAt>400&&circleHits(b,sp.x,sp.y,sp.radius??34))this.bounce(sp);
    }
    for(const sw of this.switches){
      const active=!sw.challengeId||this.challenge.active?.def.challengeId===sw.challengeId;
      sw.sp.setAlpha(!active?.2:sw.used?.46:1);sw.sp.y=sw.y+(sw.switchKind==='firefly'?Math.sin(this.simTime*.003+sw.x)*4:0);
      if(!active)continue;
      const inside=circleHits(b,sw.x,sw.y,sw.radius??22);
      if(inside&&!(sw.once&&sw.used)&&this.simTime-sw.lastAt>=(sw.cooldownMs??350)){
        // A repeatable switch retriggers on a new entry; standing inside never keeps a bridge alive forever.
        if(!sw.wasInside){sw.used=true;sw.lastAt=this.simTime;
          if(sw.groupId)this.activateGroup(sw.groupId,sw.activeDurationMs??TUNING.bridgeDuration);
          this.challenge.emit('switch',sw.id);this.audio.play('switch');this.effects.burst(sw.x,sw.y,0xf2dfa2,8);
          this.combo.add(1,'switch');}
      }
      sw.wasInside=inside;
    }
    for(const p of this.photoMarkers){const active=this.challenge.active?.def.photoId===p.photoId,found=this.save.hasPhoto(p.photoId);p.sp.setAlpha(active?1:found?.62:.5);p.sp.y=p.y+Math.sin(this.simTime*.002+p.x)*3;}
  }
  activateGroup(groupId,duration){for(const p of this.temporary)if(p.config.groupId===groupId)p.activate(this.simTime+duration);}
  activateChallengeExit(cid,duration){for(const p of this.temporary)if(p.config.challengeId===cid&&p.body.enable)p.activate(this.simTime+duration);}
  resetChallengeObjects(cid){for(const p of this.temporary)if(p.config.challengeId===cid)p.reset();
    for(const sw of this.switches)if(sw.challengeId===cid){sw.used=false;sw.wasInside=false;sw.lastAt=-1e9;}
    for(const sp of this.springs)if(sp.challengeId===cid){sp.lastAt=-1e9;this.airTouched.delete(sp.id);this.bounceCredited.delete(sp.id);}}
  updateHazards(){
    const b=this.actor.body;
    for(const h of this.hazards){
      if(h.distance){const t=this.simTime/1000*(h.speed||60)/(h.distance||60)+(h.phase||0)*Math.PI*2,offset=Math.sin(t)*h.distance;
        h.x=h.startX+(h.axis==='horizontal'?offset:0);h.y=h.startY+(h.axis==='vertical'?offset:0);h.sp.setPosition(h.x,h.y);}
      const hit=h.hazardKind==='hammer'?circleHits(b,h.x,h.y,27):overlaps(b,{x:h.x-h.rect.width/2,y:h.y-h.rect.height/2,width:h.rect.width,height:h.rect.height});
      if(hit){if(h.response==='slow')this.actor.setVelocityX(this.actor.body.velocity.x*.6);else if(h.response==='bounce')this.movement.launch(h.impulse??-520);else this.damage.hit(h);}
    }
    for(const p of this.platforms)if(p.config.sideDamage&&b.bottom>p.body.top+14&&overlaps(b,{x:p.body.left,y:p.body.top+6,width:p.body.width,height:p.height+10}))this.damage.hit({damage:p.config.sideDamage,hazardKind:'skateboard',response:'knockback'});
  }
  updateTriggers(){
    const b=this.actor.body;
    for(const c of this.checkpoints){if(c.reached||!overlaps(b,c.rect)||c.order<this.respawnPoint.order)continue;
      c.reached=true;this.respawnPoint={x:c.respawnX,y:c.respawnY,facing:c.respawnFacing||1,order:c.order};this.damage.heal(c.heal??1);
      c.sp.setTint(0xd7e0b4);this.audio.play('checkpoint');this.effects.burst(c.sp.x,c.sp.y-55,0xd6dcb2,12);this.toast('这里记住了你的脚步 · 心情 +1',1800);
      this.telemetry.emit('checkpoint_reached',{order:c.order,x:Math.round(b.center.x),happiness:this.happiness,combo:this.combo.count});}
    for(const tr of this.story){const inside=overlaps(b,tr.rect);if(inside&&!tr.inside&&(!tr.once||!tr.used)&&this.simTime-tr.lastAt>=(tr.cooldownMs??0)){
      tr.used=true;tr.lastAt=this.simTime;this.storyModule=tr.moduleIndex;this.ui.section(tr.text,tr.moduleIndex);}
      tr.inside=inside;}
    for(const r of this.branches)if(!r.used&&overlaps(b,r.rect)){r.used=true;this.telemetry.emit('route_branch_enter',{branch:r.id,route:r.route});}
  }
  placePlayer(x,feetY,{facing=1,release=false}={}){
    this.actor.body.reset(x,feetY-27);this.actor.setVelocity(0,0);this.actor.body.setGravityY(0);this.movement.reset();this.movement.facing=facing;
    this.airTouched.clear();this.landingSurface=null;if(release)this.app.input.releaseAll();
    this.avatar.setPosition(x,feetY+2).setAlpha(1).clearTint();
  }
  respawn(){this.challenge.cancel();this.placePlayer(this.respawnPoint.x,this.respawnPoint.y,{facing:this.respawnPoint.facing,release:true});this.damage.invulnerableUntil=this.simTime+1200;}
  toast(text,duration){this.ui.showToast(text,duration);}
  shake(amount,duration){if(!this.save.data.settings.reducedShake)this.cameras.main.shake(duration,amount);}
  capturePhoto(id){try{this.game.renderer.snapshot(image=>{try{const c=document.createElement('canvas');c.width=320;c.height=180;c.getContext('2d').drawImage(image,0,0,320,180);this.save.saveThumbnail(id,c.toDataURL('image/jpeg',.73));}catch{};});}catch{/* Procedural chapter postcard remains a readable fallback. */}}
  setPaused(value,reason='manual'){
    if(this.finished||this.paused===value)return;this.paused=value;this.app.input.releaseAll();this.movement.jumpUntil=-1;this.movement.cutAllowed=false;
    if(value){this.physics.pause();this.tweens.pauseAll();this.audio.pause();this.app.input.enabled=false;this.ui.pause(this);}
    else{this.ui.closeModal();this.app.input.enabled=true;this.audio.resume();this.tweens.resumeAll();this.physics.resume();}
  }
  renderAvatar(){
    const b=this.actor.body,grounded=b.blocked.down||b.touching.down;this.avatar.setPosition(b.center.x,b.bottom+3).setFlipX(this.movement.facing<0);
    let pose=grounded?(Math.abs(b.velocity.x)>15?'run':'idle'):(b.velocity.y<0?'jump':'fall');
    if(this.simTime<this.damage.invulnerableUntil-650)pose='hurt';
    if(pose==='run')this.avatar.play(`${this.character.id}-run`,true);else{if(this.avatar.anims.isPlaying)this.avatar.stop();this.avatar.setTexture(`${this.character.id}-${pose}`);}
    this.shadow.setPosition(b.center.x,b.bottom+2).setVisible(grounded).setAlpha(.12);
    this.damage.update();
  }
  finish(){
    if(this.finished)return;this.finished=true;this.app.input.releaseAll();this.app.input.enabled=false;this.challenge.cancel();
    this.physics.pause();this.actor.setVelocity(0,0);this.audio.play('finish');this.avatar.setVisible(false);this.otherAvatar.setVisible(false);this.shadow.setVisible(false);
    const y=this.goalObject.y+this.goalObject.height;this.hug=this.add.image(this.goalObject.x+47,y+3,'reunion').setOrigin(.5,1).setDisplaySize(180,150).setAlpha(0).setDepth(24);
    this.tweens.add({targets:this.hug,alpha:1,duration:750,ease:'Sine.easeOut'});this.effects.burst(this.hug.x,y-100,0xf7d295,30);
    const caption=this.ui.nodes?.['reunion-caption'];if(caption){caption.style.opacity='1';if(this.level.order!==3){caption.textContent='把这份快乐，带到下一段路。';caption.style.color='#9c805d';caption.style.textShadow='0 1px 4px #fff1d5';}}
    this.finalRun={happiness:this.happiness,maxCombo:this.combo.max,elapsed:this.simTime,hearts:this.damage.hearts,damageHits:this.damage.hits,perfect:this.perfect.count,photos:[...this.runPhotos],character:this.character.id};
    this.finalResult=this.save.commitRun(this.level,this.character.id,this.finalRun);
    this.telemetry.emit('level_finish',{levelId:this.level.id,...this.finalRun});
  }
  update(time,delta){
    if(!this.actor||this.paused)return;
    const dt=Math.min(100,Math.max(0,delta));
    if(this.finished){this.finishElapsed+=dt;this.effects.update(dt);this.backdrop.update(this.cameras.main);if(this.finishElapsed>(this.goalConfig.reunion?3900:1900))this.app.result(this.level,this.finalRun,this.finalResult);return;}
    this.simTime+=dt;this.combo.update(dt);this.photoFlash=Math.max(0,this.photoFlash-dt);
    for(const p of this.platforms)p.update(dt);
    const modifier=this.modifiersAtPlayer();this.movement.update(dt,modifier);
    if(modifier.bounce){modifier.bounce.lastAt=this.simTime;this.movement.launch(modifier.bounce.impulse??-520);this.audio.play('spring');this.effects.burst(this.actor.x,this.actor.body.bottom,0xc0ded5,8);}
    this.challenge.update(dt);this.collectAndSwitch(dt);this.updateHazards();this.updateTriggers();
    if(this.actor.body.top>850)this.damage.hit({response:'checkpoint',damage:1,hazardKind:'fall'});
    this.renderAvatar();this.effects.update(dt);this.trailClock+=dt;
    if(this.combo.count>=10&&this.trailClock>95&&Math.abs(this.actor.body.velocity.x)>30){this.trailClock=0;this.effects.trail(this.actor.x-12*this.movement.facing,this.actor.body.center.y,0xf6d99a);}
    this.audio.ambient(this.simTime,this.level.theme,this.level.order===3&&this.actor.x>8100);
    this.backdrop.update(this.cameras.main);this.ui.updateHUD(this,dt);
    if(this.actor.x>=this.goalObject.x&&(this.actor.body.blocked.down||this.actor.body.touching.down)&&this.actor.body.bottom>this.goalObject.y)this.finish();
    this.landingSurface=null;
  }
}
