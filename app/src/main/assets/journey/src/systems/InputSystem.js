const KEYS={ArrowLeft:'left',KeyA:'left',ArrowRight:'right',KeyD:'right',ArrowUp:'jump',KeyW:'jump',Space:'jump'};
/** Pointer ownership, not a single boolean: releasing one finger cannot cancel another. */
export class InputSystem {
  constructor({onPause=()=>{},onUnlock=()=>{}}={}){
    this.keys=new Set();this.pointers=new Map();this.jumpQueued=false;this.enabled=false;
    this.onPause=onPause;this.onUnlock=onUnlock;this.cleanups=[];
    this.listen(window,'keydown',e=>{
      if(e.code==='Escape'&&!e.repeat){e.preventDefault();this.onPause('manual');return;}
      if(!KEYS[e.code]||!this.enabled||/INPUT|TEXTAREA/.test(e.target?.tagName))return;
      e.preventDefault();this.onUnlock();if(!this.keys.has(e.code)&&KEYS[e.code]==='jump')this.jumpQueued=true;this.keys.add(e.code);
    });
    this.listen(window,'keyup',e=>{this.keys.delete(e.code);});
    this.listen(window,'pointerup',e=>this.releasePointer(e.pointerId));
    this.listen(window,'pointercancel',e=>{const action=this.pointers.get(e.pointerId)?.action;this.releasePointer(e.pointerId);if(action==='jump')this.jumpQueued=false;});
    this.listen(window,'blur',()=>{this.releaseAll();if(this.enabled)this.onPause('background');});
    this.listen(document,'visibilitychange',()=>{this.releaseAll();if(document.hidden&&this.enabled)this.onPause('background');});
    this.listen(window,'pagehide',()=>{this.releaseAll();if(this.enabled)this.onPause('background');});
    this.listen(document,'pointerdown',()=>this.onUnlock(),{passive:true});
    this.listen(window,'resize',()=>{this.releaseAll();if(innerHeight>innerWidth&&this.enabled)this.onPause('orientation');});
  }
  listen(target,type,fn,opts){target.addEventListener(type,fn,opts);this.cleanups.push(()=>target.removeEventListener(type,fn,opts));}
  bindControls(root){
    for(const el of root.querySelectorAll('[data-control]')){
      const down=e=>{if(!this.enabled)return;e.preventDefault();e.stopPropagation();this.onUnlock();
        // Each physical finger owns exactly one action. Adding a jump finger must not
        // replace the already-held movement finger.
        this.pointers.set(e.pointerId,{action:el.dataset.control,element:el});if(el.dataset.control==='jump')this.jumpQueued=true;
        el.classList.add('held');try{el.setPointerCapture(e.pointerId);}catch{};};
      const up=e=>{e.preventDefault();e.stopPropagation();this.releasePointer(e.pointerId);};
      const cancel=e=>{e.preventDefault();e.stopPropagation();const action=this.pointers.get(e.pointerId)?.action;this.releasePointer(e.pointerId);if(action==='jump')this.jumpQueued=false;};
      const lost=e=>this.releasePointer(e.pointerId);
      el.addEventListener('pointerdown',down);el.addEventListener('pointerup',up);el.addEventListener('pointercancel',cancel);el.addEventListener('lostpointercapture',lost);
      el._disposeInput=()=>{el.removeEventListener('pointerdown',down);el.removeEventListener('pointerup',up);el.removeEventListener('pointercancel',cancel);el.removeEventListener('lostpointercapture',lost);};
    }
  }
  releasePointer(id){const p=this.pointers.get(id);this.pointers.delete(id);if(p&&!Array.from(this.pointers.values()).some(x=>x.element===p.element))p.element.classList.remove('held');}
  releaseAll(){this.keys.clear();for(const id of [...this.pointers.keys()])this.releasePointer(id);this.jumpQueued=false;}
  held(action){return this.enabled&&([...this.keys].some(k=>KEYS[k]===action)||[...this.pointers.values()].some(p=>p.action===action));}
  get direction(){return Number(this.held('right'))-Number(this.held('left'));}
  takeJump(){const q=this.jumpQueued;this.jumpQueued=false;return this.enabled&&q;}
  destroy(){this.releaseAll();for(const f of this.cleanups)f();}
}
