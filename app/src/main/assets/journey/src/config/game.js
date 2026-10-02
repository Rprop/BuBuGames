export const VIEW = { width: 960, height: 540 };
export const TUNING = Object.freeze({ gravity: 1150, speed: 230, jumpBuffer: 150, jumpCut: -245,
  invulnerability: 900, maxHearts: 3, bridgeDuration: 4500, bridgeWarning: 1000,
  bridgeGrace: 300, maxParticles: 100 });
export const CHARACTERS = Object.freeze({
  bubu: { id: 'bubu', name: '布布', jump: -515, coyote: 180, airJumps: 0,
    comboMs: 3800, perfectRatio: .45, color: '#c98760', subtitle: '稳稳落地',
    description: '想稳一点、想多拿 Perfect、想把连击续得更久，就选布布。' },
  yier: { id: 'yier', name: '一二', jump: -505, coyote: 130, airJumps: 1,
    comboMs: 3200, perfectRatio: .32, color: '#e696a4', subtitle: '轻盈再跳',
    description: '怕跳不过去、想在空中补救路线、想玩得更轻巧，就选一二。' }
});
export const LEVELS = [
  { id: 'level_01_commute', order: 1, title: '下班冲冲冲', subtitle: '借物前进 · 城市节奏',
    theme: 'commute', chapter: '晚霞', flow: 12, totalCollectibles: 20, totalPhotos: 3,
    nextLevel: 'level_02_park', accent: '#da8c63', color: '#f3d9be',
    modules: ['街口出发', '街口与公交', '纸箱与小屋', '晚霞扶梯', '水洼慢行', '高架天桥', '摄影支路', '回家方向'] },
  { id: 'level_02_park', order: 2, title: '乐园蹦蹦跳', subtitle: '弹跳接力 · 空中连锁',
    theme: 'park', chapter: '乐园', flow: 15, totalCollectibles: 20, totalPhotos: 3,
    nextLevel: 'level_03_starlight', accent: '#739d92', color: '#dce9d7',
    modules: ['乐园入口', '彩虹气球', '木马接力', '泡泡跳台', '摩天轮脚下', '空中游行', '摄影高台', '终点合影'] },
  { id: 'level_03_starlight', order: 3, title: '星空去见你', subtitle: '点亮星桥 · 晚风小径',
    theme: 'starlight', chapter: '星空', flow: 14, totalCollectibles: 20, totalPhotos: 3,
    nextLevel: null, accent: '#a9b8df', color: '#dee3f1',
    modules: ['晚风初起', '萤火虫树', '第一座星桥', '月光坡道', '流星山坡', '风与荆棘', '灯串营地', '终点见你'] }
];
export const LEVEL_BY_ID = Object.fromEntries(LEVELS.map(l => [l.id, l]));
const photoInfo = [
  ['晚霞车站', '沿摄影小路借公交站台前进，最后跳上发光相机台。', '晚霞和公交，都想多陪我们一站。'],
  ['纸箱小屋', '踩上滚动纸箱，经过雨棚与屋顶，到达摄影点。', '普普通通的小纸箱，也装得下温柔的愿望。'],
  ['下班路灯', '依次点亮三盏路灯，再跑上摄影台。', '一盏一盏亮起来的，是回家的方向。'],
  ['彩虹气球', '顺着红、黄、蓝气球连续弹跳，中途别落回地面。', '把小小的烦恼，交给彩虹和风吧。'],
  ['旋转木马', '依次落上三匹木马，最后一次落在亮起的中心。', '世界转了一圈，身边仍然是你。'],
  ['乐园合影', '用弹跳接力集齐五个幸福事件，再到摄影台合影。', '想把这一秒的快乐，存进很久以后的回忆。'],
  ['萤火虫树', '依次点亮四只萤火虫，让星桥出现后登上树冠摄影台。', '小小的萤火，也能为我们照亮整棵树。'],
  ['流星山坡', '十二秒内收齐五颗流星星点，登上山坡。', '没来得及许愿，因为你已经在身边。'],
  ['露营灯串', '依次点亮三盏营灯，再走到灯串下合影。', '灯串尽头的小帐篷，就是今晚的宇宙。']
];
export const PHOTOS = LEVELS.flatMap((l, li) => photoInfo.slice(li*3,li*3+3).map((p,i) => ({
  id: `${l.id}_photo_${i+1}`, levelId: l.id, name: p[0], hint: p[1], caption: p[2], index: i+1, theme: l.theme
})));
export const PHOTO_BY_ID = Object.fromEntries(PHOTOS.map(p => [p.id,p]));
export const BADGES = { happiness: '满载而归', steady: '一路稳稳', flow: '心流时刻' };
export const asset = path => `./assets/${path}`;
