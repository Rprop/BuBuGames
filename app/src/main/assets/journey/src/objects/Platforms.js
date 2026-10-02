import { platformCanvas } from '../art/Artwork.js';
export class Platform {
  constructor(scene,o,p={}){
    this.s=scene;this.config=p;this.object=o;this.width=o.width;this.height=o.height||20;
    const key=`body-${Math.ceil(o.width)}-${Math.ceil(this.height)}`;
    if(!scene.textures.exists(key)){const c=document.createElement('canvas');c.width=Math.ceil(o.width);c.height=Math.ceil(this.height);c.getContext('2d').fillRect(0,0,c.width,c.height);scene.textures.addCanvas(key,c);}
    const actor=scene.physics.add.image(o.x+o.width/2,o.y+this.height/2,key).setVisible(false);
    actor.body.setAllowGravity(false);actor.body.setImmovable(true);actor.body.friction.set(1,0);
    actor.body.setMaxVelocity(1000,1000);actor.meta={id:o.name,oneWay:p.oneWay!==false,perfectEnabled:!!p.perfectEnabled,comboValue:p.comboValue??1,
      ...p,moving:!!p.moving,platform:this};
    this.bodyObject=actor;this.body=actor.body;
    const artKey=`surface-${scene.level.theme}-${p.kind||'ledge'}-${o.width}`;
    if(!scene.textures.exists(artKey))scene.textures.addCanvas(artKey,platformCanvas(scene.level.theme,p.kind||'ledge',o.width,this.height));
    this.art=scene.add.image(actor.x,o.y-4,artKey).setOrigin(.5,0).setDepth(5);
    if(p.perfectEnabled){
      this.mark=scene.add.graphics().setDepth(6);const ratio=scene.character.perfectRatio+(p.moving?.08:0);
      this.mark.lineStyle(3,scene.level.theme==='starlight'?0xf4dfad:0xecc881,.95);this.mark.lineBetween(-o.width*ratio/2,-1,o.width*ratio/2,-1);
      this.mark.fillStyle(0xfff7cf,.95);this.mark.fillCircle(0,-1,2.4);
    }
    scene.surfaces.push(actor);scene.platforms.push(this);
  }
  update(){this.art.setPosition(this.body.center.x,this.body.top-4);if(this.mark)this.mark.setPosition(this.body.center.x,this.body.top);}
  setEnabled(value){this.body.enable=value;this.art.setAlpha(value?1:.19);if(this.mark)this.mark.setAlpha(value?1:.2);}
}
export class MovingPlatform extends Platform {
  constructor(scene,o,p){super(scene,o,{...p,moving:true});this.axis=p.axis==='vertical'?'y':'x';this.start=this.bodyObject[this.axis];this.end=this.start+(p.distance||0);
    this.speed=Math.max(1,p.speed||50);this.sign=1;this.stopped=false;
    if(p.phase){const phase=(p.phase%1)*2;this.sign=phase<=1?1:-1;const ratio=phase<=1?phase:2-phase;
      const v=this.start+(this.end-this.start)*ratio;this.body.reset(this.axis==='x'?v:this.bodyObject.x,this.axis==='y'?v:this.bodyObject.y);}
  }
  update(dt){
    const b=this.body;if(this.s.simTime<(this.config.startDelayMs||0)||this.stopped){b.setVelocity(0,0);super.update();return;}
    const at=this.axis==='x'?b.center.x:b.center.y,target=this.sign>0?this.end:this.start;
    let distance=(target-at)*this.sign;
    if(distance<.2){
      if(this.sign>0&&this.config.pingPong===false){this.stopped=true;b.setVelocity(0,0);super.update();return;}
      this.sign*=-1;distance=Math.abs((this.sign>0?this.end:this.start)-at);
    }
    const v=this.sign*Math.min(this.speed,distance/Math.max(.001,dt/1000));
    b.setVelocity(this.axis==='x'?v:0,this.axis==='y'?v:0);super.update();
  }
}
export class TemporaryPlatform extends Platform {
  constructor(scene,o,p){super(scene,o,p);this.until=-1;this.expiredStanding=false;this.setEnabled(false);}
  activate(until){this.until=Math.max(this.until,until);this.expiredStanding=false;this.setEnabled(true);}
  reset(){this.until=-1;this.expiredStanding=false;this.setEnabled(false);}
  update(){
    const now=this.s.simTime,remaining=this.until-now;
    if(this.body.enable&&remaining<=0){
      if(!this.expiredStanding){this.expiredStanding=true;const a=this.s.actor.body;
        this.graceUntil=Math.abs(a.bottom-this.body.top)<10&&a.right>this.body.left&&a.left<this.body.right?this.until+300:this.until;}
      if(now>=this.graceUntil)this.setEnabled(false);
    }else if(this.body.enable){const warning=remaining<1000;this.art.setAlpha(warning?(Math.sin(now*.025)>0?.96:.42):1);}
    super.update();
  }
}
