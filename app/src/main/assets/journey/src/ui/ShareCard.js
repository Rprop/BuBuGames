import { postcardCanvas } from '../art/Artwork.js';
export function downloadBlob(blob,name){const a=document.createElement('a'),url=URL.createObjectURL(blob);a.href=url;a.download=name;document.body.append(a);a.click();a.remove();setTimeout(()=>URL.revokeObjectURL(url),1000);}
export function downloadJSON(text,name){downloadBlob(new Blob([text],{type:'application/json;charset=utf-8'}),name);}
export function downloadMemoryCard(app,level,run=null){
  const c=document.createElement('canvas');c.width=900;c.height=1050;const g=c.getContext('2d');
  g.fillStyle='#faf1de';g.fillRect(0,0,900,1050);g.strokeStyle='#d7c5a5';g.lineWidth=2;g.strokeRect(35,35,830,980);
  g.font='20px "Microsoft YaHei",sans-serif';g.fillStyle='#ac9271';g.textAlign='center';g.fillText('BUBU & YIER  /  A LITTLE JOURNEY',450,99);
  g.font='bold 45px "Microsoft YaHei",sans-serif';g.fillStyle='#7a6049';g.fillText(level.title,450,176);
  const chars=['bubu','yier'].map(id=>app.game.textures.get(`${id}-celebrate`).getSourceImage());
  const pic=postcardCanvas(level.theme,chars);g.drawImage(pic,75,217,750,437);
  g.font='25px "Microsoft YaHei",sans-serif';g.fillStyle='#9d8568';g.fillText('我想把去见你的这段路，走得更漂亮。',450,720);
  const best=app.save.data.levels[level.id].best,r=run||{happiness:best.happiness,maxCombo:best.combo,elapsed:best.timeMs};
  g.font='20px "Microsoft YaHei",sans-serif';g.fillText(`小幸福 ${r.happiness}/${level.totalCollectibles}     最高连击 ×${r.maxCombo}     照片 ${app.save.data.levels[level.id].photos.length}/${level.totalPhotos}`,450,792);
  g.font='16px "Microsoft YaHei",sans-serif';g.fillStyle='#b8a389';g.fillText('布布一二 · 去见你的小幸福',450,936);
  c.toBlob(b=>{if(b)downloadBlob(b,`去见你_${level.title}_纪念卡.png`);},'image/png');
}

/** All nine photographs unlock a separate, exportable reunion card. */
export function downloadFinalCard(app){
  const c=document.createElement('canvas');c.width=900;c.height=1050;const g=c.getContext('2d');
  const bg=g.createLinearGradient(0,0,0,1050);bg.addColorStop(0,'#3b4766');bg.addColorStop(1,'#798e96');g.fillStyle=bg;g.fillRect(0,0,900,1050);
  g.strokeStyle='#d9cda9';g.lineWidth=2;g.strokeRect(35,35,830,980);
  g.fillStyle='#efe2b8';for(let i=0;i<65;i++){const x=65+((i*173)%770),y=65+((i*239)%810);g.globalAlpha=.25+(i%4)*.15;g.beginPath();g.arc(x,y,1.4+(i%3)*.5,0,Math.PI*2);g.fill();}g.globalAlpha=1;
  g.textAlign='center';g.font='20px "Microsoft YaHei",sans-serif';g.fillStyle='#f0e1bb';g.fillText('BUBU & YIER  /  ALL NINE MOMENTS',450,99);
  g.font='bold 43px "Microsoft YaHei",sans-serif';g.fillText('幸福原来，就是你',450,180);
  const hug=app.game.textures.get('reunion').getSourceImage(),ratio=Math.min(640/hug.width,495/hug.height),w=hug.width*ratio,h=hug.height*ratio;
  g.drawImage(hug,450-w/2,260+(495-h)/2,w,h);
  g.font='24px "Microsoft YaHei",sans-serif';g.fillText('九个小瞬间，拼成了我们的整段旅程。',450,821);
  g.font='17px "Microsoft YaHei",sans-serif';g.fillText('布布一二 · 去见你的小幸福  /  09 / 09',450,936);
  c.toBlob(b=>{if(b)downloadBlob(b,'去见你_九张照片最终合影.png');},'image/png');
}
