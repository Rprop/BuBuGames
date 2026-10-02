import { props, objects } from '../tiled/LevelValidator.js';
import { PHOTO_BY_ID } from '../config/game.js';
const contains=(a,b)=>a.right>b.x&&a.left<b.x+b.width&&a.bottom>b.y&&a.top<b.y+b.height;
export class ChallengeManager {
  constructor(scene,map){this.s=scene;this.defs=objects(map,'challengeTriggers').map(o=>({...props(o),rect:o,sequence:JSON.parse(props(o).sequence||'[]')}));
    this.active=null;this.cooldowns=new Map();this.retryCount=0;
  }
  start(def){
    this.active={def,elapsed:0,step:0,seen:new Set(),count:0};this.s.resetChallengeObjects(def.challengeId);
    this.s.telemetry.emit('photo_challenge_start',{challengeId:def.challengeId,photoId:def.photoId});
    this.s.toast(`${PHOTO_BY_ID[def.photoId].name} · ${PHOTO_BY_ID[def.photoId].hint}`,3500);
  }
  emit(type,id,{perfect=false}={}){
    const a=this.active;if(!a)return;
    if(a.def.eventCount>0&&['bounce','switch','perfect','collect'].includes(type)&&!a.seen.has(id)){
      a.seen.add(id);a.count++;this.s.effects.floatText(this.s.actor.x,this.s.actor.body.top-20,`${Math.min(a.count,a.def.eventCount)} / ${a.def.eventCount}`,0xadbfa3,18);
    }
    const expect=a.def.sequence[a.step];if(expect&&expect.type===type&&expect.id===id){
      if(expect.perfect&&!perfect){this.fail('最后一匹木马：试着落在金色中心线');return;}
      a.step++;if(type==='landing'&&!perfect)this.s.combo.add(1,'challenge_step');this.s.audio.play('switch');
    }
  }
  onLanding(platform,perfect){
    const a=this.active;if(!a)return;
    if(platform?.meta?.ground&&a.def.failOnGround){this.fail('落回安全路了，再试一次就好');return;}
    if(platform)this.emit('landing',platform.meta.id,{perfect});
  }
  get progress(){const a=this.active;return !a?{done:0,total:0}:a.def.eventCount?{done:Math.min(a.count,a.def.eventCount),total:a.def.eventCount}:{done:a.step,total:a.def.sequence.length};}
  fail(reason){
    const a=this.active;if(!a)return;const d=a.def;this.active=null;this.retryCount++;
    this.cooldowns.set(d.challengeId,this.s.simTime+d.retryDelayMs);this.s.combo.break('challenge_retry');this.s.perfect.break();
    this.s.resetChallengeObjects(d.challengeId);this.s.placePlayer(d.entryX,d.entryY,{facing:1,release:true});
    this.s.toast(`${reason} · 1 秒后可重试，向下回到主路可跳过`,2200);
    this.s.telemetry.emit('photo_challenge_fail',{challengeId:d.challengeId,reason});
  }
  success(){
    const a=this.active;if(!a)return;const d=a.def;this.active=null;this.cooldowns.set(d.challengeId,this.s.simTime+4500);
    const fresh=this.s.save.addPhoto(this.s.level.id,d.photoId);this.s.runPhotos.add(d.photoId);
    this.s.audio.play('photo');this.s.effects.burst(d.finishX,d.finishY-60,0xffe3a5,22);this.s.photoFlash=300;
    this.s.toast(`${fresh?'新照片已放进相册':'又拍下了这个瞬间'} · ${PHOTO_BY_ID[d.photoId].name}`,2600);
    this.s.telemetry.emit('photo_challenge_success',{challengeId:d.challengeId,photoId:d.photoId,timeMs:Math.round(a.elapsed)});
    this.s.capturePhoto(d.photoId);
    // Keep the finish platform alive long enough to walk out; do not drop the player after a successful shot.
    this.s.activateChallengeExit(d.challengeId,2500);
  }
  cancel(){if(this.active)this.s.resetChallengeObjects(this.active.def.challengeId);this.active=null;}
  update(dt){
    const s=this.s,b=s.actor.body;
    if(!this.active){
      for(const d of this.defs){if(s.simTime<(this.cooldowns.get(d.challengeId)||0))continue;
        if(contains(b,d.rect)&&b.bottom<=d.entryY+(d.entryTolerance??24)){this.start(d);break;}}
      return;
    }
    const a=this.active,d=a.def;a.elapsed+=dt;
    if(a.elapsed>d.timeLimitMs){this.fail('拍照时间到了，刚才已经很接近啦');return;}
    if(b.bottom>d.boundsBottom||b.center.x<d.boundsLeft||b.center.x>d.boundsRight){this.fail('摄影小路在这里，重新出发吧');return;}
    const ready=this.progress.done>=this.progress.total;
    if(ready&&Math.abs(b.center.x-d.finishX)<65&&Math.abs(b.bottom-d.finishY)<16&&(b.blocked.down||b.touching.down))this.success();
  }
}
