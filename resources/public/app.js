window.onload = () => {

  const $btnJoin = document.getElementById('join');
  const $btnLeave = document.getElementById('leave');
  const $game = document.getElementById('game');
  const $gameover = document.getElementById('gameover');
  const $inName = document.getElementById('in-name');
  const $name = document.getElementById('name');
  const $players = document.getElementById('players');

  let socket = null;

  connect();

  document.onkeydown = (e) => {
      e = e || window.event;
      if (e.target !== $inName) {
        e.preventDefault();
      }
      switch (e.keyCode) {
        case 38:
          socket.send(JSON.stringify({
            type: 'action',
            payload: {
              action: 'move',
              direction: 'up',
            }
          }));
          break;
        case 40:
          socket.send(JSON.stringify({
            type: 'action',
            payload: {
              action: 'move',
              direction: 'down',
            }
          }));
          break;
        case 37:
          socket.send(JSON.stringify({
            type: 'action',
            payload: {
              action: 'move',
              direction: 'left',
            }
          }));
          break;
        case 39:
          socket.send(JSON.stringify({
            type: 'action',
            payload: {
              action: 'move',
              direction: 'right',
            }
          }));
          break;
        case 32:
          socket.send(JSON.stringify({
            type: 'action',
            payload: {
              action: 'plant-bomb',
            }
          }));
          break;
        default:
          break;
      }
  }
  
  function connect() {
    if (socket !== null) {
      console.error('already connected');
      return;
    }
  
    let uri = 'ws://' + location.host + location.pathname;
    uri = uri.substring(0, uri.lastIndexOf('/'));
    socket = new WebSocket(uri);
  
    socket.onerror = (error) => {
      console.error(error);
    }
  
    socket.onopen = (event) => {
      console.log('connected to ' + event.currentTarget.url);
    }
  
    socket.onmessage = (event) => {
      const {type, payload} = JSON.parse(event.data);
      switch (type) {
        case 'error':
          console.error(payload);
          break;
        case 'message':
          console.log(payload);
          break;
        case 'refresh':
          refresh(payload);
          break;
        default:
          console.error('invalid event: ' + event);
          break;
      }
    }
  
    socket.onclose = (event) => {
      console.log('disconnected: ' + event.code + ' ' + JSON.parse(event.reason).message);
      socket = null;
    }
  }
  
  $btnJoin.onclick = (e) => {
    if (socket === null) {
      console.error('not connected');
      return;
    }
    if ($inName.value.trim().length === 0) {
      $inName.focus();
    } else {
      socket.send(JSON.stringify({type: 'join', payload: {name: $inName.value}}));
    }
  }
  
  $btnLeave.onclick = (e) => {
    if (socket === null) {
      console.error('not connected');
      return;
    }
    socket.send(JSON.stringify({type: 'leave'}));
  }

  function clearGame(inProgress) {
    hideGameover();
    while ($game.lastChild && $game.childNodes.length > 1) {
      $game.removeChild($game.lastChild);
    }
    $game.className = inProgress ? 'in-progress' : '';
  }

  function getCellClassName(cell, players) {
    if (cell === null) {
      return 'cell empty';
    }
    const classes = ['cell'];
    if ('block' in cell) {
      classes.push('block');
      classes.push(cell.block.type);
      if (cell.block.hit) {
        classes.push('hit');
      }
    }
    if ('bomb' in cell && !('detonated' in cell.bomb)) {
      classes.push('bomb');
    }
    if ('item' in cell) {
      classes.push('item');
      classes.push('item-' + cell.item.type);
      if (cell.item.hit) {
        classes.push('hit');
      }
    }
    if ('fire' in cell) {
      classes.push('fire');
    }
    if ('player-id' in cell) {
      const playerId = cell['player-id'];
      const player = players[playerId];
      classes.push(playerId)
      if (player.hit) {
        classes.push('hit');
      }
    }
    return classes.join(' ');
  }

  function makeRowDiv(row, players) {
    const rowDiv = document.createElement('div');
    rowDiv.className = 'row';
    row.forEach(cell => {
      const cellDiv = document.createElement('span');
      cellDiv.className = getCellClassName(cell, players);
      rowDiv.append(cellDiv);
    });

    return rowDiv;
  }

  function refreshGame(payload) {
    const {
      width,
      height,
      grid: {v},
      'in-progress?': inProgress,
      players,
    } = payload;

    $name.style.display = 'none';
    clearGame(inProgress);
    for (let i = 0; i < height; i++) {
      const startIdx = i * width;
      const rowDiv = makeRowDiv(v.slice(startIdx, startIdx + width), players);
      $game.appendChild(rowDiv);
    }
  }

  function hideGameover() {
    $gameover.style.display = 'none';
  }

  function showGameover(text) {
    if ($gameover.style.display === 'flex') {
      return;
    }
    if ($gameover.lastChild) {
      $gameover.removeChild($gameover.lastChild);
    }
    $gameover.appendChild(document.createTextNode(text));
    $gameover.style.display = 'flex';
  }

  function refreshGameover(payload) {
    const {
      gameover,
      players,
    } = payload;

    if (gameover) {
      const text = gameover.winner ?
        `${players[gameover.winner].name} wins!` :
        `no winner`;
      showGameover(text);
    } else {
      hideGameover();
    }
  }

  function refreshPlayers(payload) {
    const {'num-players': numPlayers, players} = payload;

    const curNumPlayers = Object.keys(players).length;
    const text = `${curNumPlayers}/${numPlayers} ` +
      (curNumPlayers < numPlayers ? 'players waiting' : 'playing');
    if ($players.lastChild) {
      $players.removeChild($players.lastChild);
    }
    $players.appendChild(document.createTextNode(text));
  }

  function refresh(payload) {
    const {'in-progress?': inProgress} = payload;

    refreshPlayers(payload)
    if (inProgress) {
      refreshGame(payload);
      refreshGameover(payload);
    } else {
      clearGame(inProgress);
      $name.style.display = 'block';
    }
  }
}
