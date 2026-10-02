import Phaser from '../lib/phaser.js';
import { LEVELS, asset } from '../config/game.js';
import { installArtwork } from '../art/Artwork.js';
export class BootScene extends Phaser.Scene {
  constructor(){super('Boot');}
  preload(){
    this.errors=[];for(const c of ['bubu','yier'])for(const pose of ['idle','run-a','run-b','run-c','run-d','jump','fall','hurt','celebrate'])this.load.image(`${c}-${pose}`,asset(`characters/${c}-${pose}.png`));
    this.load.image('reunion',asset('characters/reunion.png'));
    for(const l of LEVELS)this.load.json(l.id,asset(`maps/${l.id}.json`));
    this.load.on('progress',v=>{const e=document.querySelector('#boot-progress i');if(e)e.style.width=`${v*100}%`;});
    this.load.on('loaderror',f=>this.errors.push(f.key));
  }
  create(){
    const app=this.game.registry.get('app');
    if(this.errors.length){const p=document.getElementById('boot-progress');p.innerHTML='<strong>有几件行李没装好</strong><small>请通过本地预览服务器或网站地址打开，不要直接双击 index.html。</small><button onclick="location.reload()">重新装载</button>';console.error('Missing assets',this.errors);return;}
    installArtwork(this);
    for(const c of ['bubu','yier'])this.anims.create({key:`${c}-run`,frames:['a','b','c','d'].map(x=>({key:`${c}-run-${x}`})),frameRate:10,repeat:-1});
    app.ui.prepareArt();document.getElementById('boot-progress').style.display='none';
    this.scene.start('Menu',{screen:'title'});
  }
}
