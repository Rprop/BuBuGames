import { LEVELS, PHOTOS } from '../config/game.js';
export const props=o=>Object.fromEntries((o?.properties||[]).map(p=>[p.name,p.value]));
export const layer=(map,name)=>map.layers.find(l=>l.name===name);
export const objects=(map,name)=>layer(map,name)?.objects||[];
export function groundRects(map){
  const t=layer(map,'terrain'),out=[];if(!t)return out;
  let current=null;
  for(let x=0;x<map.width;x++){
    let top=null;for(let y=0;y<map.height;y++)if(t.data[y*map.width+x]){top=y*map.tileheight;break;}
    if(top===null){current=null;continue;}
    if(current&&current.y===top)current.width+=map.tilewidth;
    else {current={id:`ground-${x}`,x:x*map.tilewidth,y:top,width:map.tilewidth,height:map.height*map.tileheight-top};out.push(current);}
  }
  return out;
}
export function validateLevel(map,level){
  const errors=[],warnings=[];
  if(map.width!==192||map.height!==16||map.tilewidth!==48||map.tileheight!==48)errors.push('地图尺寸应为192×16');
  for(const name of ['spawn','goal','cameraBounds'])if(objects(map,name).length!==1)errors.push(`${name}必须且只能有一个`);
  const next=props(objects(map,'goal')[0]).nextLevel;if(next&&!LEVELS.some(l=>l.id===next))errors.push('goal.nextLevel 指向不存在的关卡');
  const cs=objects(map,'collectibles');if(cs.reduce((n,c)=>n+Number(props(c).value??1),0)!==level.totalCollectibles)errors.push('小幸福数量与manifest不一致');
  const photoIds=objects(map,'photos').map(o=>props(o).photoId),known=PHOTOS.filter(p=>p.levelId===level.id).map(p=>p.id);
  if(photoIds.length!==level.totalPhotos||new Set(photoIds).size!==photoIds.length||photoIds.some(id=>!known.includes(id)))errors.push('照片总数、唯一性或ID错误');
  const names=new Set(map.layers.flatMap(l=>(l.objects||[]).map(o=>o.name)));
  const groups=new Set(objects(map,'temporaryPlatforms').map(o=>props(o).groupId));
  for(const o of objects(map,'switches')){const p=props(o);if(p.groupId&&!groups.has(p.groupId))errors.push(`开关${o.name}无对应平台组`);}
  const enums={movingPlatforms:{axis:['horizontal','vertical']},modifiers:{modifierKind:['wind','conveyor','slow','slippery','bounce']},hazards:{response:['knockback','checkpoint','slow','bounce']},switches:{switchKind:['spark','meteor','lamp','firefly','star']}};
  for(const [ln,fields]of Object.entries(enums))for(const o of objects(map,ln))for(const [field,allowed]of Object.entries(fields))if(!allowed.includes(props(o)[field]))errors.push(`${o.name}.${field}无效`);
  for(const o of objects(map,'challengeTriggers')){
    const p=props(o);if(!photoIds.includes(p.photoId))errors.push(`挑战${o.name}缺照片`);
    if(!(p.retryDelayMs>=0&&p.entryX>=0&&p.entryY>0))errors.push(`挑战${o.name}缺安全重试入口`);
    try{for(const ev of JSON.parse(p.sequence))if(!names.has(ev.id))errors.push(`挑战步骤${ev.id}不存在`);}catch{errors.push(`${o.name}步骤不是JSON数组`);}
  }
  const cp=objects(map,'checkpoints').map(o=>props(o).order).sort((a,b)=>a-b);
  if(cp.join(',')!=='1,2,3')errors.push('检查点order需连续1/2/3');
  for(const o of objects(map,'perfectZones'))if(!names.has(props(o).platformId))errors.push(`落点${o.name}缺平台`);
  const floor=groundRects(map);let maxGap=0,maxRise=0;
  for(let i=1;i<floor.length;i++){const gap=floor[i].x-floor[i-1].x-floor[i-1].width,rise=floor[i-1].y-floor[i].y;maxGap=Math.max(maxGap,gap);maxRise=Math.max(maxRise,rise);if(gap>144||rise>96)errors.push(`主线几何风险x=${floor[i].x}: gap=${gap},rise=${rise}`);}
  if(objects(map,'perfectZones').length!==10)warnings.push('Perfect区数量不是10');
  return {ok:errors.length===0,errors,warnings,stats:{happiness:cs.length,photos:photoIds.length,checkpoints:cp.length,perfect:objects(map,'perfectZones').length,maxGap,maxRise}};
}
