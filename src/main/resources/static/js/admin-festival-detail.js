var festivalLat = _geoData.lat;
var festivalLng = _geoData.lng;
var existingBooths = _geoData.booths;

let boothMap, selectedMarker;
const markerColors = { FOOD: '#FF7043', BEER: '#FFA000', EVENT: '#7B1FA2' };
const boothIcons  = { FOOD: '🍽', BEER: '🍺', EVENT: '🎉' };

function createBoothEl(booth) {
    const color = markerColors[booth.boothType] || '#555';
    const icon  = boothIcons[booth.boothType]  || '📍';

    const card = document.createElement('div');
    card.style.cssText = [
        'background:' + color,
        'color:#fff',
        'border-radius:8px',
        'font-size:12px',
        'font-weight:700',
        'box-shadow:0 2px 8px rgba(0,0,0,0.35)',
        'border:2px solid rgba(255,255,255,0.75)',
        'cursor:pointer',
        'user-select:none',
        'overflow:hidden',
        'width:90px',
        'text-align:center'
    ].join(';');

    if (booth.imageUrl) {
        const img = document.createElement('img');
        img.src = booth.imageUrl;
        img.style.cssText = 'width:90px;height:auto;display:block;';
        card.appendChild(img);
    }

    const label = document.createElement('div');
    label.style.padding = '4px 8px';
    label.textContent = icon + ' ' + booth.name;
    card.appendChild(label);

    const tail = document.createElement('div');
    tail.style.cssText = [
        'width:0',
        'height:0',
        'border-left:6px solid transparent',
        'border-right:6px solid transparent',
        'border-top:8px solid ' + color,
        'margin:0 auto'
    ].join(';');

    const wrapper = document.createElement('div');
    wrapper.style.textAlign = 'center';
    wrapper.appendChild(card);
    wrapper.appendChild(tail);
    return wrapper;
}

function createSelectedEl() {
    const el = document.createElement('div');
    el.style.cssText = [
        'width:16px',
        'height:16px',
        'background:#3b5bdb',
        'border:3px solid #fff',
        'border-radius:50%',
        'box-shadow:0 2px 6px rgba(0,0,0,0.4)'
    ].join(';');
    return el;
}

function initBoothMap() {
    const centerLat = festivalLat != null ? festivalLat : 37.5665;
    const centerLng = festivalLng != null ? festivalLng : 126.9780;

    boothMap = new google.maps.Map(document.getElementById('boothMap'), {
        center: { lat: centerLat, lng: centerLng },
        zoom: 18,
        mapTypeControl: false,
        streetViewControl: false,
        mapId: 'DEMO_MAP_ID'
    });

    existingBooths.forEach(function(booth) {
        const marker = new google.maps.marker.AdvancedMarkerElement({
            position: { lat: booth.latitude, lng: booth.longitude },
            map: boothMap,
            title: booth.name,
            content: createBoothEl(booth)
        });
        const descHtml = booth.description
            ? '<br><span style="font-weight:400;font-size:12px;color:#555;">' + booth.description + '</span>'
            : '';
        const infoWindow = new google.maps.InfoWindow({
            content: '<div style="padding:8px 12px;font-size:13px;font-weight:600;">'
                + booth.boothTypeName + ' · ' + booth.name + descHtml + '</div>'
        });
        marker.addListener('click', function() {
            infoWindow.open({ anchor: marker, map: boothMap });
        });
    });

    boothMap.addListener('click', function(e) {
        const lat = e.latLng.lat();
        const lng = e.latLng.lng();
        document.getElementById('boothLat').value = lat;
        document.getElementById('boothLng').value = lng;
        const label = document.getElementById('boothPosLabel');
        label.textContent = '선택된 위치: ' + lat.toFixed(6) + ', ' + lng.toFixed(6);
        label.style.color = '#3b5bdb';
        label.style.fontWeight = 'normal';
        document.getElementById('boothMapWrap').style.borderColor = 'var(--border)';
        if (selectedMarker) selectedMarker.map = null;
        selectedMarker = new google.maps.marker.AdvancedMarkerElement({
            position: e.latLng,
            map: boothMap,
            content: createSelectedEl()
        });
    });
}

function validateBoothImage(input) {
    const errEl = document.getElementById('boothImageError');
    const allowed = ['.jpg', '.jpeg', '.png', '.gif'];
    const maxBytes = 10 * 1024 * 1024;
    if (!input.files || !input.files[0]) { errEl.style.display = 'none'; return true; }
    const file = input.files[0];
    const ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
    if (!allowed.includes(ext)) {
        errEl.textContent = '⚠ jpg, jpeg, png, gif 파일만 업로드할 수 있습니다.';
        errEl.style.display = 'block';
        input.value = '';
        return false;
    }
    if (file.size > maxBytes) {
        errEl.textContent = '⚠ 파일 크기는 10MB를 초과할 수 없습니다.';
        errEl.style.display = 'block';
        input.value = '';
        return false;
    }
    errEl.style.display = 'none';
    return true;
}

function validateBoothPos() {
    const imageInput = document.getElementById('boothImageFile');
    if (imageInput && imageInput.files && imageInput.files[0] && !validateBoothImage(imageInput)) {
        imageInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
        return false;
    }
    if (!document.getElementById('boothLat').value) {
        const label = document.getElementById('boothPosLabel');
        label.textContent = '⚠ 지도를 클릭하여 부스 위치를 먼저 선택해주세요.';
        label.style.color = '#e53e3e';
        label.style.fontWeight = '600';
        document.getElementById('boothMapWrap').style.borderColor = '#e53e3e';
        document.getElementById('boothMapWrap').scrollIntoView({ behavior: 'smooth', block: 'center' });
        return false;
    }
    return true;
}

function validateTimetable(form) {
    const start = form.querySelector('#startTime').value;
    const end = form.querySelector('#endTime').value;
    if (start && end && start >= end) {
        const err = document.getElementById('timeError');
        err.style.display = 'block';
        document.getElementById('startTime').style.borderColor = '#e53e3e';
        document.getElementById('endTime').style.borderColor = '#e53e3e';
        document.getElementById('endTime').focus();
        return false;
    }
    return true;
}

function validateCustomTimetable(form) {
    const start = form.querySelector('[name="startTime"]').value;
    const end = form.querySelector('[name="endTime"]').value;
    if (start && end && start >= end) {
        alert('종료 시간은 시작 시간보다 늦어야 합니다.');
        return false;
    }
    return true;
}

function clearTimeError() {
    const err = document.getElementById('timeError');
    if (err) err.style.display = 'none';
    document.getElementById('startTime').style.borderColor = '';
    document.getElementById('endTime').style.borderColor = '';
}

// ── 스테이지 색상 ──
const stageColors = [
    { bg: '#3b5bdb', light: '#eef2ff', border: '#c5d0fa' },
    { bg: '#e8590c', light: '#fff4e6', border: '#ffc9a0' },
    { bg: '#2b8a3e', light: '#ebfbee', border: '#b2f2bb' },
    { bg: '#be4bdb', light: '#f8f0fc', border: '#e599f7' },
    { bg: '#1098ad', light: '#e6fcf5', border: '#96f2d7' },
    { bg: '#f59f00', light: '#fff9db', border: '#ffe066' },
    { bg: '#d6336c', light: '#fff0f6', border: '#fcc2d7' },
    { bg: '#495057', light: '#f1f3f5', border: '#ced4da' },
];
(function applyStageColors() {
    const stageMap = {};
    document.querySelectorAll('.stage-row[data-stage]').forEach(function(row, i) {
        const name = row.getAttribute('data-stage');
        const c = stageColors[i % stageColors.length];
        stageMap[name] = c;
        row.style.borderLeft = '4px solid ' + c.bg;
        row.style.background = c.light;
        row.style.borderColor = c.border;
        row.style.borderLeftColor = c.bg;
        row.querySelector('.stage-order-num').style.background = c.bg;
    });
    document.querySelectorAll('.badge-stage[data-stage]').forEach(function(badge) {
        const name = badge.getAttribute('data-stage');
        const c = stageMap[name];
        if (c) {
            badge.style.background = c.bg;
            badge.style.color = '#fff';
            badge.style.padding = '3px 10px';
            badge.style.borderRadius = '20px';
            badge.style.fontSize = '12px';
            badge.style.fontWeight = '600';
        }
    });
})();

function onArtistChange(select) {
    const opt = select.options[select.selectedIndex];
    const stage = opt.getAttribute('data-stage') || '';
    document.getElementById('autoStageName').value = stage;
    const preview = document.getElementById('stagePreview');
    if (stage) {
        preview.textContent = stage;
        preview.style.color = '#3b5bdb';
        preview.style.fontWeight = '600';
    } else {
        preview.textContent = '스테이지 미지정';
        preview.style.color = 'var(--muted)';
        preview.style.fontWeight = 'normal';
    }
}
