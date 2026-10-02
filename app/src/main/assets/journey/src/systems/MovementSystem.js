import { TUNING } from '../config/game.js';
export class MovementSystem {
  constructor(scene,actor,character,input){this.s=scene;this.actor=actor;this.c=character;this.input=input;this.reset();}
  reset(){this.lastGround=-1e9;this.jumpUntil=-1;this.airJumps=0;this.airborne=false;this.airTime=0;
    this.lastVy=0;this.flight=0;this.airOriginY=this.actor.body.bottom;this.airTravel=0;this.lockUntil=0;this.boostUntil=0;this.boostX=0;this.cutAllowed=false;this.facing=1;}
  launch(vy,vx=null,boostMs=0){
    this.actor.setVelocityY(vy);this.airborne=true;this.airTime=0;this.airOriginY=this.actor.body.bottom;this.airTravel=0;this.flight++;this.airJumps=0;this.lastGround=-1e9;
    this.cutAllowed=false;this.jumpUntil=-1;
    if(vx!==null){this.actor.setVelocityX(vx);this.boostX=vx;this.boostUntil=this.s.simTime+boostMs;}
  }
  update(dt,modifier){
    const b=this.actor.body,now=this.s.simTime,grounded=b.blocked.down||b.touching.down;
    if(grounded){
      this.lastGround=now;this.airJumps=0;
      if(this.airborne&&this.airTime>100&&this.airTravel>16){const bounced=this.s.onLanding(this.s.landingSurface,this.lastVy,this.flight);if(bounced){this.lastVy=b.velocity.y;return;}}
      this.airborne=false;this.airTime=0;
    }else{if(!this.airborne){this.airborne=true;this.flight++;this.airOriginY=b.bottom;this.airTravel=0;}this.airTime+=dt;this.airTravel=Math.max(this.airTravel,Math.abs(b.bottom-this.airOriginY));}
    if(this.input.takeJump())this.jumpUntil=now+TUNING.jumpBuffer;
    b.setGravityY(modifier.extraGravityY??0);
    if(now<this.lockUntil){this.lastVy=b.velocity.y;return;}
    let vx=this.input.direction*TUNING.speed*(modifier.moveMultiplier??1);
    if(!grounded)vx*=modifier.airControlMultiplier??1;
    if(now<this.boostUntil&&this.input.direction>=0)vx=this.boostX;
    else if(grounded)vx+=modifier.conveyor??0;
    if(modifier.slippery&&grounded){vx=b.velocity.x+(vx-b.velocity.x)*Math.min(1,dt/180);}
    b.setVelocityX(vx);b.setGravityY(modifier.extraGravityY??0);
    if(this.input.direction)this.facing=this.input.direction;
    if(this.jumpUntil>=now){
      if(grounded||now-this.lastGround<=this.c.coyote){
        b.setVelocityY(this.c.jump);this.airborne=true;this.airTime=0;this.airOriginY=b.bottom;this.airTravel=0;this.flight++;
        this.lastGround=-1e9;this.jumpUntil=-1;this.cutAllowed=true;this.s.audio.play('jump');
      }else if(this.airJumps<this.c.airJumps){
        this.airJumps++;b.setVelocityY(this.c.jump*.92);this.jumpUntil=-1;this.cutAllowed=true;
        this.s.effects.burst(b.center.x,b.bottom,0xe4c9ed,7);this.s.audio.play('jump');
      }
    }
    if(this.cutAllowed&&!this.input.held('jump')&&b.velocity.y<TUNING.jumpCut)b.setVelocityY(TUNING.jumpCut);
    this.lastVy=b.velocity.y;
  }
}
