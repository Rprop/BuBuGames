export class Telemetry {
  constructor(){this.events=[];this.runId=`run-${Date.now().toString(36)}`;this.clock=()=>0;}
  emit(type,detail={}){this.events.push({runId:this.runId,type,t:Math.round(this.clock()),...detail});if(this.events.length>3000)this.events.shift();}
  exportJSON(){return JSON.stringify({schema:1,network:'none',events:this.events},null,2);}
}
