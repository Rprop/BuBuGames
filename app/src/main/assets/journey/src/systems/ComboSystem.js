export class ComboSystem {
  constructor(durationMs,{onAdd=()=>{},onBreak=()=>{}}={}){
    this.durationMs=durationMs;this.count=0;this.max=0;this.remaining=0;this.onAdd=onAdd;this.onBreak=onBreak;
  }
  add(value=1,reason='happiness'){
    this.count+=Math.max(0,Math.floor(value));this.max=Math.max(this.max,this.count);this.remaining=this.durationMs;
    this.onAdd(this.count,reason);return this.count;
  }
  update(dt){if(this.count>0){this.remaining=Math.max(0,this.remaining-dt);if(!this.remaining)this.break('timeout');}}
  break(reason){if(this.count)this.onBreak(reason,this.count);this.count=0;this.remaining=0;}
  get ratio(){return this.count?this.remaining/this.durationMs:0;}
}
