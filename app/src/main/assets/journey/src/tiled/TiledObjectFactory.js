import { objects,props,groundRects } from './LevelValidator.js';
import { Platform, MovingPlatform, TemporaryPlatform } from '../objects/Platforms.js';
import { drawGround } from '../art/Artwork.js';
export class TiledObjectFactory {
  constructor(scene,map){this.s=scene;this.map=map;}
  build(){
    const s=this.s;s.surfaces=[];s.platforms=[];s.temporary=[];s.tokens=[];s.springs=[];s.switches=[];s.hazards=[];s.modifiers=[];s.photoMarkers=[];
    for(const r of groundRects(this.map)){
      const block=s.add.rectangle(r.x+r.width/2,r.y+r.height/2,r.width,r.height,0x000000,0).setVisible(false);s.physics.add.existing(block,true);
      block.meta={ground:true,id:r.id,oneWay:false};s.surfaces.push(block);drawGround(s,r,s.level.theme);
    }
    const zoneByPlatform=new Map(objects(this.map,'perfectZones').map(o=>[props(o).platformId,props(o)]));
    for(const name of ['platforms','movingPlatforms','temporaryPlatforms'])for(const o of objects(this.map,name)){
      const p={...props(o),...zoneByPlatform.get(o.name)};
      const Cls=name==='movingPlatforms'?MovingPlatform:name==='temporaryPlatforms'?TemporaryPlatform:Platform;
      const inst=new Cls(s,o,p);if(name==='temporaryPlatforms')s.temporary.push(inst);
    }
    for(const o of objects(this.map,'collectibles')){
      const p=props(o),sp=s.add.image(o.x+o.width/2,o.y+o.height/2,'icon-happy').setDisplaySize(42,42).setDepth(12);
      s.tokens.push({id:o.name,x:sp.x,y:sp.y,sp,...p,collected:false});
    }
    for(const o of objects(this.map,'springs')){
      const p=props(o),balloon=p.kind==='balloon';let sp;
      if(balloon){
        const key=`balloon-${p.color}`;
        if(!s.textures.exists(key)){const c=document.createElement('canvas');c.width=80;c.height=116;const g=c.getContext('2d');
          g.strokeStyle='#a39a83';g.lineWidth=1.6;g.beginPath();g.moveTo(40,70);g.bezierCurveTo(28,90,54,98,38,114);g.stroke();
          g.fillStyle=p.color;g.beginPath();g.ellipse(40,34,27,32,0,0,7);g.fill();g.strokeStyle='rgba(106,87,74,.5)';g.stroke();
          g.fillStyle='rgba(255,255,240,.65)';g.beginPath();g.ellipse(31,22,6,10,.35,0,7);g.fill();g.fillStyle=p.color;g.beginPath();g.moveTo(40,64);g.lineTo(35,75);g.lineTo(45,75);g.fill();s.textures.addCanvas(key,c);}
        sp=s.add.image(o.x+o.width/2,o.y+o.height/2, key).setOrigin(.5,.3).setDisplaySize(66,96).setDepth(12);
      }else{
        const platform=new Platform(s,o,{...p,oneWay:true,kind:'candy',springId:o.name});platform.art.setVisible(false);
        sp=s.add.image(o.x+o.width/2,o.y+o.height/2,'icon-spring').setOrigin(.5,.58).setDisplaySize(94,55).setDepth(8);
      }
      s.springs.push({id:o.name,...p,x:o.x+o.width/2,y:o.y+o.height/2,sp,balloon,lastAt:-1e9});
    }
    for(const o of objects(this.map,'switches')){
      const p=props(o),icon=['spark','meteor'].includes(p.switchKind)?'star':p.switchKind==='lamp'?'lamp':'fly';
      const sp=s.add.image(o.x+o.width/2,o.y+o.height/2,`icon-${icon}`).setDisplaySize(icon==='star'?27:44,icon==='star'?27:44).setDepth(13);
      s.switches.push({id:o.name,...p,x:sp.x,y:sp.y,sp,used:false,wasInside:false,lastAt:-1e9});
    }
    for(const o of objects(this.map,'modifiers')){
      const p=props(o),g=s.add.graphics().setDepth(3),night=s.level.theme==='starlight';
      if(p.modifierKind==='wind'){
        g.lineStyle(2,0xe1dfc6,.25);for(let i=0;i<11;i++){const y=o.y+28+i*31,x=o.x+(i*71)%170;g.beginPath();g.moveTo(x,y);g.lineTo(x+220,y-22);g.strokePath();}
        s.add.text(o.x+45,o.y+o.height-84,'顺着晚风，跳得轻一点',{fontFamily:'Microsoft YaHei',fontSize:'15px',color:'#ece6c8'}).setDepth(3).setAlpha(.8);
      }else if(p.modifierKind==='conveyor'){
        g.fillStyle(0xadac97,.7);g.fillRoundedRect(o.x,o.y+8,o.width,13,5);g.lineStyle(2,0x757a6d,.55);
        for(let x=o.x+12;x<o.x+o.width;x+=32){g.lineBetween(x-p.direction*5,o.y+10,x+p.direction*5,o.y+15);g.lineBetween(x+p.direction*5,o.y+15,x-p.direction*5,o.y+20);}
      }else{
        g.fillStyle(p.modifierKind==='bounce'?0xafd9d5:night?0xa1bfca:0xaccfd0,.6);g.fillEllipse(o.x+o.width/2,o.y+12,o.width,18);
        g.lineStyle(2,0xf9ffff,.5);g.strokeEllipse(o.x+o.width*.42,o.y+11,o.width*.45,5);
        if(p.modifierKind==='bounce')for(let i=0;i<5;i++){g.lineStyle(1.5,0xf5ffec,.85);g.strokeCircle(o.x+25+i*37,o.y+2-(i%3)*7,8+(i%2)*4);}
      }
      s.modifiers.push({id:o.name,rect:o,...p,g,lastAt:-1e9});
    }
    for(const o of objects(this.map,'hazards')){
      const p=props(o),sp=s.add.graphics().setDepth(11),item={id:o.name,rect:o,...p,sp,x:o.x+o.width/2,y:o.y+o.height/2,startX:o.x+o.width/2,startY:o.y+o.height/2};
      if(p.hazardKind==='hammer'){
        sp.lineStyle(7,0xab9e81,.9);sp.lineBetween(0,-66,0,0);sp.fillStyle(0xe0aaaa,1);sp.fillRoundedRect(-29,-22,58,44,20);sp.lineStyle(2,0xa97870,.8);sp.strokeRoundedRect(-29,-22,58,44,20);sp.fillStyle(0xf4d5c6,.8);sp.fillEllipse(-8,-5,18,10);
      }else{
        sp.fillStyle(0x829095,1);for(let x=-o.width/2;x<o.width/2;x+=20)sp.fillTriangle(x,12,x+10,-12,x+20,12);
      }sp.setPosition(item.x,item.y);s.hazards.push(item);
    }
    s.checkpoints=objects(this.map,'checkpoints').map(o=>{
      const p=props(o),sp=s.add.image(o.x+25,o.y+o.height,'icon-flag').setOrigin(.5,1).setDisplaySize(62,76).setDepth(9);return{id:o.name,rect:o,...p,sp,reached:false};
    });
    for(const o of objects(this.map,'photos')){const p=props(o),sp=s.add.image(o.x+o.width/2,o.y+o.height/2,'icon-camera').setDisplaySize(50,50).setDepth(15);s.photoMarkers.push({...p,x:sp.x,y:sp.y,sp});}
    for(const o of objects(this.map,'challengeTriggers')){
      const p=props(o);s.add.image(p.entryX-7,p.entryY-79,'icon-camera').setDisplaySize(36,36).setDepth(11).setAlpha(.86);
      s.add.text(p.entryX,p.entryY-109,'跳上来 · 拍一张',{fontFamily:'Microsoft YaHei',fontSize:'14px',color:s.level.theme==='starlight'?'#eee3c8':'#927767',backgroundColor:s.level.theme==='starlight'?'#465873cc':'#fff4ded9',padding:{x:7,y:4}}).setOrigin(.5).setDepth(12);
    }
    s.story=objects(this.map,'storyTriggers').map(o=>({rect:o,...props(o),used:false,inside:false,lastAt:-1e9}));
    s.branches=objects(this.map,'routeBranches').map(o=>({id:o.name,rect:o,...props(o),used:false}));
    s.goalObject=objects(this.map,'goal')[0];s.goalConfig=props(s.goalObject);
  }
}
