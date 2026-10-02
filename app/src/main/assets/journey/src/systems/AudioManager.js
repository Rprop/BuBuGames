/** A small procedural music box; no remote audio or copyrighted songs. */
export class AudioManager {
  constructor(save){this.save=save;this.ctx=null;this.master=null;this.notes=new Set();this.lastAmbient=0;this.suspended=false;}
  unlock(){
    if(!this.ctx){const AC=window.AudioContext||window.webkitAudioContext;if(!AC)return;
      try{this.ctx=new AC();this.master=this.ctx.createGain();this.master.gain.value=.15;this.master.connect(this.ctx.destination);}catch{return;}}
    if(this.ctx.state==='suspended'&&!this.suspended)this.ctx.resume().catch(()=>{});
  }
  tone(freq,duration=.13,type='sine',gain=.4,delay=0){
    if(!this.ctx||this.save.data.settings.muted||this.suspended||this.ctx.state!=='running')return;
    const start=this.ctx.currentTime+delay,osc=this.ctx.createOscillator(),env=this.ctx.createGain();
    osc.type=type;osc.frequency.setValueAtTime(freq,start);env.gain.setValueAtTime(.0001,start);
    env.gain.exponentialRampToValueAtTime(gain,start+.008);env.gain.exponentialRampToValueAtTime(.0001,start+duration);
    osc.connect(env);env.connect(this.master);this.notes.add(osc);
    osc.onended=()=>{osc.disconnect();env.disconnect();this.notes.delete(osc);};osc.start(start);osc.stop(start+duration+.025);
  }
  play(kind,combo=0){
    const scale=[523.25,587.33,659.25,783.99,880,1046.5];
    if(kind==='collect')this.tone(scale[Math.min(5,Math.floor(combo/4))],.17,'sine',.32);
    else if(kind==='jump')this.tone(290,.065,'sine',.15);
    else if(kind==='perfect'){this.tone(659,.18);this.tone(988,.22,'sine',.28,.045);}
    else if(kind==='spring'){this.tone(392,.08,'triangle',.28);this.tone(784,.18,'sine',.3,.045);}
    else if(kind==='photo'){for(let i=0;i<3;i++)this.tone([1046,1318,1568][i],.2,'sine',.28,i*.06);}
    else if(kind==='damage')this.tone(130,.15,'triangle',.25);
    else if(kind==='checkpoint'||kind==='finish'){[523,659,784,1046].forEach((f,i)=>this.tone(f,.32,'sine',.3,i*.11));}
    else if(kind==='switch')this.tone(880,.19,'sine',.28);
    else if(kind==='milestone'){this.tone(784,.18);this.tone(1175,.28,'sine',.3,.07);}
  }
  ambient(time,theme,quiet=false){if(time-this.lastAmbient<2100)return;this.lastAmbient=time;
    const seq=theme==='starlight'?[392,523,587,659,523,440]:theme==='park'?[523,659,784,659,587,440]:[440,523,659,587,523,392];
    this.tone(seq[Math.floor(time/2100)%seq.length],1.7,'sine',quiet?.025:.06);
  }
  pause(){this.suspended=true;for(const o of this.notes){try{o.stop();}catch{}}}
  resume(){this.suspended=false;this.unlock();}
}
