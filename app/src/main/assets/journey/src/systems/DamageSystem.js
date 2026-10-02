import { TUNING } from '../config/game.js';
export class DamageSystem {
  constructor(scene){this.s=scene;this.hearts=3;this.hits=0;this.invulnerableUntil=0;}
  hit(config={}){
    if(this.s.finished||this.s.paused)return;
    if(this.s.challenge.active){this.s.challenge.fail('没关系，摄影小路再试一次');return;}
    const now=this.s.simTime,fall=config.response==='checkpoint';
    if(!fall&&now<this.invulnerableUntil)return;
    const damage=Math.max(0,Number(config.damage??1));
    if(damage>0){this.hearts=Math.max(0,this.hearts-damage);this.hits++;this.s.combo.break(fall?'fall':'damage');this.s.perfect.break();}
    this.invulnerableUntil=now+(config.cooldownMs??TUNING.invulnerability);
    this.s.audio.play('damage');this.s.shake(.003,150);
    this.s.telemetry.emit(fall?'player_fall':'player_damage',{hazardKind:config.hazardKind||'fall',x:Math.round(this.s.actor.x),hearts:this.hearts});
    if(fall||this.hearts<=0){
      const exhausted=this.hearts<=0;if(exhausted)this.hearts=3;
      this.s.respawn();this.s.toast(exhausted?'抱抱，休息好就从这里继续。':'没关系，刚才的检查点还在。');
    }else{
      const vx=config.knockbackX??-this.s.movement.facing*190;
      this.s.actor.setVelocity(vx,config.knockbackY??-260);this.s.movement.lockUntil=now+280;
    }
  }
  heal(n=1){this.hearts=Math.min(TUNING.maxHearts,this.hearts+n);}
  update(){const protectedNow=this.s.simTime<this.invulnerableUntil;
    this.s.avatar.setAlpha(protectedNow?(Math.floor(this.s.simTime/90)%2?.38:.9):1);
    if(!protectedNow)this.s.avatar.clearTint();
  }
}
