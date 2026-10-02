export class Effects {
  constructor(scene){this.s=scene;this.particles=[];this.texts=[];}
  burst(x,y,color=0xf2d28e,n=8){
    n=Math.min(n,100-this.particles.length);
    for(let i=0;i<n;i++){
      const a=i*Math.PI*2/n-1.5,v=40+Math.random()*80;
      const sp=this.s.add.image(x,y,'icon-star').setDisplaySize(7+Math.random()*5,7+Math.random()*5).setTint(color).setDepth(50);
      this.particles.push({sp,x,y,vx:Math.cos(a)*v,vy:Math.sin(a)*v-45,life:520,full:520});
    }
  }
  trail(x,y,color){if(this.particles.length>80)return;const sp=this.s.add.image(x,y,'icon-star').setDisplaySize(8,8).setTint(color).setDepth(2);
    this.particles.push({sp,x,y,vx:0,vy:8,life:300,full:300});}
  floatText(x,y,text,color=0xaf7b51,size=21){
    if(this.texts.length>8){const first=this.texts.shift();first.destroy();}
    const t=this.s.add.text(x,y,text,{fontFamily:'"Microsoft YaHei",sans-serif',fontSize:`${size}px`,fontStyle:'bold',color:`#${color.toString(16).padStart(6,'0')}`,stroke:'#fff8e8',strokeThickness:3}).setOrigin(.5).setDepth(60);
    this.texts.push(t);this.s.tweens.add({targets:t,y:y-35,alpha:0,duration:760,ease:'Sine.easeOut',onComplete:()=>{t.destroy();this.texts=this.texts.filter(x=>x!==t);}});
  }
  update(dt){for(let i=this.particles.length-1;i>=0;i--){const p=this.particles[i];p.life-=dt;if(p.life<=0){p.sp.destroy();this.particles.splice(i,1);continue;}
    p.vy+=dt*.09;p.x+=p.vx*dt/1000;p.y+=p.vy*dt/1000;p.sp.setPosition(p.x,p.y).setAlpha(p.life/p.full);}}
  destroy(){for(const p of this.particles)p.sp.destroy();for(const t of this.texts)t.destroy();this.particles=[];this.texts=[];}
}
