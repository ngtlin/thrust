async function init() {

  const registration = await navigator.serviceWorker.register('/sw.js');
  await navigator.serviceWorker.ready;	
  firebase.initializeApp({
    apiKey: "AIzaSyDNTEb9EQnNSsG57VLxZR_7RbMEFZnczA8",
    authDomain: "thrust-b086b.firebaseapp.com",
    projectId: "thrust-b086b",
    storageBucket: "thrust-b086b.appspot.com",
    messagingSenderId: "835071861863",
    appId: "1:835071861863:web:e5cb3601f3dfd8628420c2"
  });
  const messaging = firebase.messaging();
  messaging.usePublicVapidKey('BF-4W5BB1Se0Prukf001WRUsBkY7N2FW46HGYUA8wPalLu3E07RyX9q45QqnJmUQkbjakTny0uDJ8E7f6Wly8d0');
  messaging.useServiceWorker(registration);	
  
  try {
    await messaging.requestPermission();
  } catch (e) {
    console.log('Unable to get permission', e);
    return;
  }

  navigator.serviceWorker.addEventListener('message', event => {
    if (event.data === 'newData') {
      showData();
    }
  });

  const currentToken = await messaging.getToken();

  fetch('/api/thrust/fcmpush/register', {
    method: 'post',
    body: JSON.stringify({token: currentToken}),
    headers: {
      "content-type": "application/json"
    }
  });
  showData();

  messaging.onTokenRefresh(async () => {
    console.log('token refreshed');
    const newToken = await messaging.getToken();
    fetch('/api/thrust/fcmpush/register', {
      method: 'post',
      body: JSON.stringify({token: newToken}),
      headers: {
        "content-type": "application/json"
      }
    });
  });
  
}

async function showData() {
  const db = await getDb();
  const tx = db.transaction('jokes', 'readonly');
  const store = tx.objectStore('jokes');
  store.getAll().onsuccess = e => showNotifications(e.target.result);
}

function showNotifications(notifications) {
  const table = document.getElementById('outTable');

  notifications.sort((a, b) => parseInt(b.ts) - parseInt(a.ts));
  const html = [];
  notifications.forEach(j => {
    const date = new Date(parseInt(j.ts));
    html.push(`<div><div class="header">${date.toISOString()} ${j.id} (${j.seq})</div><div class="notification">${j.joke}</div></div>`);
  });
  table.innerHTML = html.join('');
}

async function getDb() {
  if (this.db) {
    return Promise.resolve(this.db);
  }
  return new Promise(resolve => {
    const openRequest = indexedDB.open("Chuck", 1);

    openRequest.onupgradeneeded = event => {
      const db = event.target.result;
      db.createObjectStore('jokes', { keyPath: 'id' });
    };

    openRequest.onsuccess = event => {
      this.db = event.target.result;
      resolve(this.db);
    }
  });
}

init();