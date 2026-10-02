export class PerfectSystem {
  constructor(scene,character){this.s=scene;this.c=character;this.count=0;this.streak=0;this.lastFlight=-1;this.lastCreditZone=null;}
  landing(platform,vy,flight){
    if(!platform||!platform.meta?.perfectEnabled||vy<45||this.lastFlight===flight){if(platform&&!platform.meta?.perfectEnabled)this.streak=0;return false;}
    this.lastFlight=flight;const b=platform.body,m=platform.meta;
    const ratio=(m.widthRatioOverride??this.c.perfectRatio)+(m.moving?.08:0);
    const dist=Math.abs(this.s.actor.body.center.x-b.center.x),perfect=dist<=b.width*ratio/2;
    if(perfect){this.count++;this.streak++;
      if(this.lastCreditZone!==m.id){this.s.combo.add(m.comboValue??1,'perfect');this.lastCreditZone=m.id;}
      this.s.effects.floatText(b.center.x,b.top-70,this.streak>=3?`PERFECT ×${this.streak}`:'PERFECT',0xc18b43,24);
      this.s.effects.burst(b.center.x,b.top,0xf6ce75,12);this.s.audio.play('perfect');
      this.s.telemetry.emit('perfect_landing',{zone:m.id,x:Math.round(b.center.x),flight});
      if(this.streak===3||this.streak===5)this.s.shake(.001,90);
    }else this.streak=0;
    return perfect;
  }
  break(){this.streak=0;this.lastCreditZone=null;this.lastFlight=-1;}
}
