importScripts('https://www.gstatic.com/firebasejs/8.3.0/firebase-app.js');
importScripts('https://www.gstatic.com/firebasejs/8.3.0/firebase-messaging.js');

firebase.initializeApp({
  apiKey: "AIzaSyDNTEb9EQnNSsG57VLxZR_7RbMEFZnczA8",
  projectId: "thrust-b086b",
  messagingSenderId: "835071861863",
  appId: "1:835071861863:web:e5cb3601f3dfd8628420c2"
});

const messaging = firebase.messaging();
messaging.usePublicVapidKey('BF-4W5BB1Se0Prukf001WRUsBkY7N2FW46HGYUA8wPalLu3E07RyX9q45QqnJmUQkbjakTny0uDJ8E7f6Wly8d0');

self.addEventListener('push', async event => {
	const db = await getDb();
	const tx = this.db.transaction('jokes', 'readwrite');
	const store = tx.objectStore('jokes');

	const data = event.data.json().data;
	data.id = parseInt(data.id);
	console.log('-XXX->Push event data, ', data)
	store.put(data);

	tx.oncomplete = async e => {
		const allClients = await clients.matchAll({ includeUncontrolled: true });
		for (const client of allClients) {
			client.postMessage('newData');
		}
	};
});

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


messaging.setBackgroundMessageHandler(function(payload) {
  console.log('Received background message ', payload);
  const notificationTitle = 'Swiss Climate Challenge';
  const notificationOptions = {
    body: 'SCC push message body',
    icon: '/mail2.png'
  };

  return self.registration.showNotification(notificationTitle,
      notificationOptions);
});


const CACHE_NAME = 'my-site-cache-v1';
const urlsToCache = [
	'/index.html',
	'/index.js',
	'/mail.png',
	'/mail2.png',
	'/manifest.json'
];

self.addEventListener('install', event => {
	event.waitUntil(caches.open(CACHE_NAME)
		.then(cache => cache.addAll(urlsToCache)));
});

self.addEventListener('fetch', event => {
	event.respondWith(
		caches.match(event.request)
			.then(response => {
				if (response) {
					return response;
				}
				return fetch(event.request);
			})
	);
});


