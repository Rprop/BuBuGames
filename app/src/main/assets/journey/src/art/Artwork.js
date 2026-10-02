export const PALETTE={
  commute:{sky:['#f7d6c0','#fff3d9'],ink:'#735147',land:'#d7d9b7',edge:'#a6b48d',soil:'#eee2c9',far:'#dbc7b9',near:'#bfafa0',accent:'#d7986c'},
  park:{sky:['#d9ebe3','#f9f2d8'],ink:'#536c62',land:'#bdd7bd',edge:'#92b6a0',soil:'#e7e6cd',far:'#c7d8c4',near:'#aac7b4',accent:'#d69c9a'},
  starlight:{sky:['#303f61','#78869c'],ink:'#35455b',land:'#7e9da5',edge:'#bdd0c5',soil:'#526778',far:'#576887',near:'#40566d',accent:'#e6d0a0'}
};
const canvas=(w,h)=>{const c=document.createElement('canvas');c.width=w;c.height=h;return c;};
const rr=(c,x,y,w,h,r,fill,stroke=null)=>{c.beginPath();c.roundRect(x,y,w,h,r);c.fillStyle=fill;c.fill();if(stroke){c.strokeStyle=stroke;c.lineWidth=1.8;c.stroke();}};
function star(c,x,y,r,fill){c.beginPath();for(let i=0;i<10;i++){const a=i*Math.PI/5-Math.PI/2,rad=i%2?r*.45:r;c.lineTo(x+Math.cos(a)*rad,y+Math.sin(a)*rad);}c.closePath();c.fillStyle=fill;c.fill();}
function cloud(c,x,y,w,alpha=.65){c.save();c.globalAlpha=alpha;c.fillStyle='#fffcf0';c.beginPath();c.ellipse(x,y,w*.5,w*.16,0,0,Math.PI*2);c.ellipse(x-w*.15,y-w*.11,w*.23,w*.19,0,0,Math.PI*2);c.ellipse(x+w*.14,y-w*.11,w*.26,w*.24,0,0,Math.PI*2);c.fill();c.restore();}
function tree(c,x,y,s,p,night=false){
  c.save();c.translate(x,y);c.scale(s,s);c.strokeStyle=night?'#4d6370':'#ac9678';c.lineWidth=8;c.lineCap='round';c.beginPath();c.moveTo(0,0);c.lineTo(0,-88);c.moveTo(0,-35);c.lineTo(-24,-63);c.stroke();
  c.fillStyle=p;c.beginPath();c.ellipse(0,-100,43,61,-.06,0,Math.PI*2);c.ellipse(-29,-80,28,36,-.2,0,Math.PI*2);c.ellipse(30,-89,26,39,.2,0,Math.PI*2);c.fill();c.restore();
}
function skyline(c){
  const colors=['#d9c8b5','#cfbaaa','#e6d4b9','#d8bca8'];
  for(let i=0;i<16;i++){const x=i*110-20,h=105+(i*71%175),y=580-h;rr(c,x,y,88,h,8,colors[i%4]);
    c.fillStyle='rgba(255,246,223,.65)';for(let a=0;a<3;a++)for(let b=0;b<Math.floor(h/35);b++)c.fillRect(x+14+a*24,y+17+b*31,11,16);
    if(i%3===1){c.strokeStyle='#b6a79b';c.lineWidth=2;c.beginPath();c.moveTo(x+12,y);c.lineTo(x+18,y-18);c.lineTo(x+48,y-18);c.stroke();}}
}
function bridge(c,x,y,w,night=false){
  c.save();c.strokeStyle=night?'#9ba9ae':'#b79f8c';c.lineWidth=7;c.lineCap='round';c.beginPath();c.moveTo(x,y);c.lineTo(x+w,y);c.stroke();
  c.lineWidth=3;c.beginPath();c.moveTo(x+20,y);c.quadraticCurveTo(x+w*.28,y-78,x+w*.46,y);c.quadraticCurveTo(x+w*.7,y-82,x+w-20,y);c.stroke();
  c.strokeStyle=night?'rgba(247,222,161,.78)':'rgba(255,229,177,.78)';c.lineWidth=2;c.beginPath();c.moveTo(x+8,y-8);c.lineTo(x+w-8,y-8);c.stroke();
  c.fillStyle='#f4d79c';for(let i=0;i<9;i++){c.beginPath();c.arc(x+18+i*(w-36)/8,y-8,3,0,7);c.fill();}c.restore();
}
function lampPost(c,x,y,s=1,night=false){
  c.save();c.translate(x,y);c.scale(s,s);c.strokeStyle=night?'#777f7d':'#927d68';c.lineWidth=5;c.lineCap='round';c.beginPath();c.moveTo(0,0);c.lineTo(0,-104);c.stroke();
  c.fillStyle=night?'#f4d89a':'#f2d69f';c.shadowColor='#f4cf83';c.shadowBlur=night?22:10;rr(c,-15,-111,30,34,7,night?'#e6d2a4':'#ead3a9',night?'#6f726b':'#917d69');c.fillStyle='#fff0b3';c.fillRect(-8,-102,16,18);c.shadowBlur=0;c.restore();
}
function observatory(c,x,y,s=.8){
  c.save();c.translate(x,y);c.scale(s,s);rr(c,-54,-70,108,70,8,'#7e899b');c.fillStyle='#abb4c0';c.beginPath();c.arc(0,-72,55,Math.PI,0);c.fill();c.strokeStyle='#66758a';c.lineWidth=4;c.beginPath();c.arc(0,-72,55,Math.PI,0);c.stroke();c.fillStyle='#f5dda4';rr(c,-10,-54,20,30,5,'#f2d79e');c.restore();
}
function waterBand(c,y,color,highlight){
  c.fillStyle=color;c.fillRect(0,y,1600,768-y);c.strokeStyle=highlight;c.lineWidth=2;c.globalAlpha=.35;for(let i=0;i<18;i++){const yy=y+18+i*9;c.beginPath();c.moveTo((i*97)%210,yy);c.bezierCurveTo(420,yy-6,780,yy+7,1500,yy-2);c.stroke();}c.globalAlpha=1;
}
function ferris(c,x,y,r,alpha=.6){
  c.save();c.globalAlpha=alpha;c.lineWidth=3;c.strokeStyle='#a9b8a5';c.beginPath();c.arc(x,y,r,0,Math.PI*2);c.stroke();c.beginPath();c.arc(x,y,r-8,0,Math.PI*2);c.stroke();
  for(let i=0;i<10;i++){const a=i*Math.PI/5,xx=x+Math.cos(a)*r,yy=y+Math.sin(a)*r;c.beginPath();c.moveTo(x,y);c.lineTo(xx,yy);c.stroke();rr(c,xx-13,yy-8,26,23,7,['#e4b6a7','#e4ce94','#adc8c4'][i%3]);}
  c.strokeStyle='#b2ba9f';c.lineWidth=11;c.beginPath();c.moveTo(x-77,y+r+65);c.lineTo(x,y);c.lineTo(x+77,y+r+65);c.stroke();c.restore();
}
function tent(c,x,y,w,color){c.fillStyle=color;c.beginPath();c.moveTo(x-w*.5,y);c.lineTo(x,y-w*.5);c.lineTo(x+w*.5,y);c.closePath();c.fill();
 c.fillStyle='#fff2d9';for(let i=-2;i<=2;i++){c.beginPath();c.moveTo(x,y-w*.5);c.lineTo(x+i*w*.16,y);c.lineTo(x+(i+.45)*w*.16,y);c.fill();}rr(c,x-w*.42,y,w*.84,w*.37,4,'#e7cfaa');
 c.fillStyle=color;c.beginPath();c.moveTo(x-w*.14,y+w*.37);c.lineTo(x,y+14);c.lineTo(x+w*.14,y+w*.37);c.fill();}
export function makeBackground(theme,layer='far'){
  const p=PALETTE[theme],c=canvas(1600,768),g=c.getContext('2d');
  if(layer==='sky'){
    const grad=g.createLinearGradient(0,0,0,768);grad.addColorStop(0,p.sky[0]);grad.addColorStop(1,p.sky[1]);g.fillStyle=grad;g.fillRect(0,0,1600,768);
    if(theme==='starlight'){
      for(let i=0;i<105;i++){const x=(i*307+71)%1600,y=(i*97+35)%410;g.globalAlpha=.2+(i%4)*.16;star(g,x,y,i%8===0?4:1.4,'#f8edca');}g.globalAlpha=1;
      g.fillStyle='#f7e5ba';g.beginPath();g.arc(790,143,54,0,Math.PI*2);g.fill();g.fillStyle=p.sky[0];g.beginPath();g.arc(811,129,47,0,Math.PI*2);g.fill();
    }else{g.fillStyle=theme==='park'?'#f3dfa3':'#edb58e';g.beginPath();g.arc(790,192,76,0,Math.PI*2);g.fill();
      for(let i=0;i<9;i++)cloud(g,80+i*198,115+(i*73%154),100+i%3*38,.58);}
  }else if(layer==='far'){
    if(theme==='commute'){
      skyline(g);waterBand(g,585,'rgba(199,202,187,.48)','rgba(255,232,190,.78)');bridge(g,340,600,760,false);
      for(let i=0;i<6;i++)lampPost(g,250+i*230,620,.55,false);
    } else if(theme==='park'){
      g.fillStyle='#ccdeca';g.beginPath();g.moveTo(0,620);for(let i=0;i<=16;i++)g.lineTo(i*100,485+Math.sin(i*.9)*40);g.lineTo(1600,768);g.lineTo(0,768);g.fill();
      ferris(g,1140,335,163,.66);ferris(g,280,431,105,.3);
      g.strokeStyle='#b3c8b1';g.lineWidth=5;g.beginPath();g.moveTo(0,525);for(let i=0;i<=40;i++)g.lineTo(i*40,500-Math.sin(i*.22)**2*100);g.stroke();
    }else{
      for(let k=0;k<2;k++){g.fillStyle=k?'#526680':'#65738d';g.beginPath();g.moveTo(0,640);for(let i=0;i<=16;i++)g.lineTo(i*100,420+k*35+Math.sin(i*.79+k)*65+Math.cos(i*1.8)*37);g.lineTo(1600,768);g.lineTo(0,768);g.fill();}
      waterBand(g,605,'rgba(65,85,111,.52)','rgba(223,211,163,.58)');bridge(g,510,615,620,true);observatory(g,1310,520,.78);
    }
  }else{
    if(theme==='commute'){
      for(let i=0;i<10;i++){const x=70+i*179;tree(g,x,677,.72+(i%3)*.16,['#c3c9a8','#b9c1a2','#d9cda8'][i%3]);}
      for(let i=0;i<4;i++){const x=260+i*390;rr(g,x,566,112,76,7,['#ddbcaa','#d1cead','#d8c6ad'][i%3]);
        rr(g,x-7,554,126,22,5,'#efdbc0');g.fillStyle='#f7eed8';g.fillRect(x+14,590,28,40);g.fillRect(x+66,587,29,29);}
      for(let i=0;i<5;i++)lampPost(g,120+i*330,708,.62,false);
    }else if(theme==='park'){
      tent(g,230,533,155,'#c99593');tent(g,780,525,200,'#86b8af');tent(g,1390,550,170,'#d7b576');
      for(let i=0;i<8;i++)tree(g,i*218+20,692,.5,'#aec9ae');
      g.strokeStyle='#9aaa94';g.lineWidth=2;g.beginPath();g.moveTo(0,425);g.quadraticCurveTo(800,555,1600,425);g.stroke();
      for(let i=0;i<7;i++)lampPost(g,80+i*250,708,.55,false);
      for(let i=0;i<30;i++){const x=i*56,y=425+Math.sin(i/30*Math.PI)*60;g.fillStyle=['#dfaf9c','#d9ca88','#9abdb0'][i%3];g.beginPath();g.moveTo(x,y);g.lineTo(x+16,y+24);g.lineTo(x+30,y+2);g.fill();}
    }else{
      for(let i=0;i<15;i++){const x=i*115,y=667,s=.6+(i%3)*.22;tree(g,x,y,s,i%2?'#405970':'#476577',true);}
      for(let i=0;i<36;i++){const x=(i*317)%1600,y=455+(i*31)%190;g.fillStyle='#f0dda0';g.globalAlpha=.38+(i%4)*.11;g.beginPath();g.arc(x,y,2.2+(i%3),0,7);g.fill();}g.globalAlpha=1;
      for(let i=0;i<5;i++)lampPost(g,150+i*340,718,.58,true);
    }
  }
  return c;
}
export function platformCanvas(theme,kind,w,h=20){
  const p=PALETTE[theme],height=kind==='bus'?74:kind==='horse'?72:kind==='box'?54:kind==='skate'?34:52;
  const c=canvas(Math.ceil(w+12),height+12),g=c.getContext('2d');g.translate(6,4);
  if(kind==='bus'){
    rr(g,0,0,w,58,12,'#e9c484',p.ink);rr(g,8,8,w-16,26,6,'#eaf0df');
    g.strokeStyle='#c59b67';g.lineWidth=3;for(let x=30;x<w-10;x+=30){g.beginPath();g.moveTo(x,9);g.lineTo(x,35);g.stroke();}
    rr(g,12,57,18,12,5,'#756c61');rr(g,w-30,57,18,12,5,'#756c61');g.fillStyle='#fff4c8';g.fillRect(w-12,42,8,8);
  }else if(kind==='box'){
    rr(g,0,0,w,46,5,'#cfaa7a',p.ink);g.fillStyle='#ead5b1';g.fillRect(w*.43,1,w*.14,45);g.strokeStyle='#af855c';g.lineWidth=2;g.strokeRect(8,8,w-16,28);
  }else if(kind==='skate'){
    rr(g,0,0,w,14,7,'#a9c9c1',p.ink);g.fillStyle='#8a8073';g.beginPath();g.arc(18,24,7,0,7);g.arc(w-18,24,7,0,7);g.fill();
  }else if(kind==='horse'){
    rr(g,0,0,w,18,9,'#eac7b1',p.ink);g.fillStyle='#d9ba9b';g.beginPath();g.ellipse(w*.5,39,29,15,0,0,7);g.fill();
    rr(g,w*.7,18,14,31,7,'#d9ba9b');g.beginPath();g.ellipse(w*.75,21,14,10,-.3,0,7);g.fill();g.strokeStyle='#d9ba9b';g.lineWidth=5;g.beginPath();g.moveTo(w*.4,46);g.lineTo(w*.3,59);g.moveTo(w*.6,46);g.lineTo(w*.68,58);g.stroke();g.fillStyle='#f5e2bc';g.fillRect(w*.48,24,5,27);
  }else if(kind==='mushroom'){
    rr(g,w*.42,15,w*.16,28,7,'#d7cdb3');rr(g,0,0,w,22,11,theme==='starlight'?'#98b2bb':'#dbb8a6',p.ink);
    g.fillStyle='#edead0';for(let i=0;i<5;i++){g.beginPath();g.arc(15+i*(w-30)/4,10,3,0,7);g.fill();}
  }else{
    const color=kind==='star'?'#c4d8d1':kind==='moon'?'#c6d0df':kind==='photo'?'#e9d4a6':kind==='candy'?'#e1bbb6':kind==='roof'?'#d1a18b':p.land;
    rr(g,0,0,w,20,8,color,theme==='starlight'?'#a8c0c6':p.ink);rr(g,5,2,w-10,5,3,'rgba(255,255,240,.65)');
    if(kind==='awning'||kind==='roof'){g.fillStyle='#ead5b8';for(let x=10;x<w;x+=25)g.fillRect(x,19,12,11);}
    if(kind==='star'){for(let i=0;i<3;i++)star(g,w*(i+1)/4,12,4,'#eff3d9');}
  }
  return c;
}
export function makeIcon(type){
  const c=canvas(80,80),g=c.getContext('2d');
  if(type==='happy'){
    g.shadowColor='#edc988';g.shadowBlur=10;g.fillStyle='#f3d291';g.beginPath();g.arc(40,40,24,0,7);g.fill();g.shadowBlur=0;g.strokeStyle='#b58b54';g.lineWidth=2;g.stroke();
    g.fillStyle='#765849';g.beginPath();g.arc(32,37,2.2,0,7);g.arc(48,37,2.2,0,7);g.fill();g.strokeStyle='#765849';g.beginPath();g.arc(40,41,8,.1,Math.PI-.1);g.stroke();
    g.fillStyle='#eeb9a3';g.beginPath();g.ellipse(24,44,4,2.5,0,0,7);g.ellipse(56,44,4,2.5,0,0,7);g.fill();
  }else if(type==='camera'){
    rr(g,10,24,60,40,10,'#fff5df','#947c65');rr(g,20,16,18,13,4,'#d4b795');g.fillStyle='#a3c8c5';g.beginPath();g.arc(42,43,14,0,7);g.fill();g.strokeStyle='#7f9d9c';g.stroke();g.fillStyle='#edf6e4';g.beginPath();g.arc(38,39,5,0,7);g.fill();g.fillStyle='#e6b18d';g.beginPath();g.arc(61,33,3,0,7);g.fill();
  }else if(type==='flag'){
    g.strokeStyle='#977e64';g.lineWidth=4;g.lineCap='round';g.beginPath();g.moveTo(24,72);g.lineTo(24,9);g.stroke();g.fillStyle='#e9b496';g.beginPath();g.moveTo(27,11);g.bezierCurveTo(44,3,53,23,71,13);g.lineTo(66,41);g.bezierCurveTo(48,51,43,25,27,39);g.fill();star(g,46,24,8,'#fff3d0');
  }else if(type==='star')star(g,40,40,28,'#fff0b5');
  else if(type==='spring'){
    g.strokeStyle='#af9e91';g.lineWidth=4;g.beginPath();g.moveTo(20,64);g.lineTo(56,48);g.lineTo(23,38);g.lineTo(54,24);g.stroke();rr(g,8,17,64,15,8,'#ddafa7','#8d756b');rr(g,8,63,64,8,4,'#bcb9a3');
  }else if(type==='fly'){
    g.shadowColor='#fce6a3';g.shadowBlur=22;g.fillStyle='#f3dfaa';g.beginPath();g.ellipse(40,40,9,15,0,0,7);g.fill();g.shadowBlur=0;g.fillStyle='rgba(240,244,209,.7)';g.beginPath();g.ellipse(28,34,12,7,.5,0,7);g.ellipse(51,34,12,7,-.5,0,7);g.fill();
  }else if(type==='lamp'){
    g.strokeStyle='#8b846d';g.lineWidth=3;g.beginPath();g.arc(40,25,12,Math.PI,0);g.stroke();rr(g,24,25,32,39,8,'#d3bd8e','#857b63');rr(g,29,31,22,25,5,'#ffedb4');
  }
  return c;
}
export function installArtwork(scene){
  for(const theme of Object.keys(PALETTE))for(const l of ['sky','far','near']){const key=`bg-${theme}-${l}`;if(!scene.textures.exists(key))scene.textures.addCanvas(key,makeBackground(theme,l));}
  for(const icon of ['happy','camera','flag','star','spring','fly','lamp'])if(!scene.textures.exists(`icon-${icon}`))scene.textures.addCanvas(`icon-${icon}`,makeIcon(icon));
  const hit=canvas(28,54);hit.getContext('2d').fillRect(0,0,28,54);scene.textures.addCanvas('actor-hitbox',hit);
  const dot=canvas(8,8);dot.getContext('2d').fillRect(0,0,8,8);scene.textures.addCanvas('pixel',dot);
}
export function addBackdrop(scene,theme){
  const sky=scene.add.tileSprite(0,0,960,768,`bg-${theme}-sky`).setOrigin(0).setScrollFactor(0).setDepth(-30);
  const far=scene.add.tileSprite(0,0,960,768,`bg-${theme}-far`).setOrigin(0).setScrollFactor(0).setDepth(-20);
  const near=scene.add.tileSprite(0,0,960,768,`bg-${theme}-near`).setOrigin(0).setScrollFactor(0).setDepth(-10);
  return {update(camera){sky.tilePositionX=camera.scrollX*.035;far.tilePositionX=camera.scrollX*.20;near.tilePositionX=camera.scrollX*.40;
    sky.tilePositionY=camera.scrollY*.08;far.tilePositionY=camera.scrollY*.22;near.tilePositionY=camera.scrollY*.60+65;}};
}
export function drawGround(scene,rect,theme){
  const p=PALETTE[theme],g=scene.add.graphics().setDepth(0),c=h=>parseInt(h.slice(1),16);
  g.fillStyle(c(p.soil));g.fillRoundedRect(rect.x,rect.y,rect.width,rect.height+90,{tl:10,tr:10,bl:0,br:0});
  g.fillStyle(c(p.edge));g.fillRoundedRect(rect.x,rect.y,rect.width,20,{tl:10,tr:10,bl:5,br:5});
  g.fillStyle(c(p.land));g.fillRoundedRect(rect.x+1,rect.y,rect.width-2,10,{tl:8,tr:8,bl:4,br:4});
  g.lineStyle(1.5,c(p.ink),.12);g.lineBetween(rect.x+6,rect.y+22,rect.x+rect.width-6,rect.y+22);
  for(let x=rect.x+28;x<rect.x+rect.width;x+=57){const y=rect.y+43+(x*7%37);g.fillStyle(c(p.ink),.07);g.fillEllipse(x,y,6,3);}
  // Quiet, low-contrast grass and tiny flowers make the surface readable without visual noise.
  for(let x=rect.x+40;x<rect.x+rect.width-30;x+=139){g.lineStyle(1.6,c(p.edge),.9);g.lineBetween(x,rect.y,x-3,rect.y-7);g.lineBetween(x,rect.y,x+4,rect.y-9);
    if(Math.floor(x/139)%3===0){g.fillStyle(theme==='starlight'?0xf0d5a1:0xe8b3a0,.9);g.fillCircle(x+5,rect.y-10,2.6);}}
  return g;
}
export function postcardCanvas(theme,avatars=[],label=''){const c=canvas(720,420),g=c.getContext('2d');
  g.drawImage(makeBackground(theme,'sky'),0,0,1600,768,0,0,875,420);g.drawImage(makeBackground(theme,'far'),0,0,1600,768,0,0,875,420);g.drawImage(makeBackground(theme,'near'),0,0,1600,768,0,0,875,420);
  g.fillStyle=PALETTE[theme].land;g.fillRect(0,354,720,66);for(let i=0;i<avatars.length;i++)if(avatars[i])g.drawImage(avatars[i],270+i*102,230,150,150);
  if(label){g.font='600 26px sans-serif';g.fillStyle=theme==='starlight'?'#f8edd8':'#796454';g.fillText(label,32,49);}return c;
}
