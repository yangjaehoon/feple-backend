(function () {
    var festivalLat = _geoData.lat;
    var festivalLng = _geoData.lng;
    var existingBooths = _geoData.booths;

    var boothMap, selectedMarker;
    var markerColors = { FOOD: '#FF7043', BEER: '#FFA000', EVENT: '#7B1FA2' };
    var boothIcons  = { FOOD: '🍽', BEER: '🍺', EVENT: '🎉' };

    function createBoothEl(booth) {
        var color = markerColors[booth.boothType] || '#555';
        var icon  = boothIcons[booth.boothType]  || '📍';

        var card = document.createElement('div');
        card.className = 'booth-marker-card';
        card.style.background = color;

        if (booth.imageUrl) {
            var img = document.createElement('img');
            img.src = booth.imageUrl;
            img.className = 'booth-marker-img';
            card.appendChild(img);
        }

        var label = document.createElement('div');
        label.className = 'booth-marker-label';
        label.textContent = icon + ' ' + booth.name;
        card.appendChild(label);

        var arrow = document.createElement('div');
        arrow.className = 'booth-marker-arrow';
        arrow.style.borderTopColor = color;

        var wrapper = document.createElement('div');
        wrapper.className = 'booth-marker-wrap';
        wrapper.appendChild(card);
        wrapper.appendChild(arrow);
        return wrapper;
    }

    function createSelectedEl() {
        var el = document.createElement('div');
        el.className = 'booth-selected-dot';
        return el;
    }

    function initBoothMap() {
        var centerLat = festivalLat != null ? festivalLat : 37.5665;
        var centerLng = festivalLng != null ? festivalLng : 126.9780;

        boothMap = new google.maps.Map(document.getElementById('boothMap'), {
            center: { lat: centerLat, lng: centerLng },
            zoom: 18,
            mapTypeControl: false,
            streetViewControl: false,
            mapId: 'DEMO_MAP_ID'
        });

        existingBooths.forEach(function (booth) {
            var marker = new google.maps.marker.AdvancedMarkerElement({
                position: { lat: booth.latitude, lng: booth.longitude },
                map: boothMap,
                title: booth.name,
                content: createBoothEl(booth)
            });
            var infoDiv = document.createElement('div');
            infoDiv.className = 'booth-info-popup';
            infoDiv.textContent = (booth.boothTypeName || '') + ' · ' + booth.name;
            if (booth.description) {
                var descEl = document.createElement('span');
                descEl.className = 'booth-info-desc';
                descEl.textContent = booth.description;
                infoDiv.appendChild(descEl);
            }
            var infoWindow = new google.maps.InfoWindow({ content: infoDiv });
            marker.addListener('click', function () {
                infoWindow.open({ anchor: marker, map: boothMap });
            });
        });

        boothMap.addListener('click', function (e) {
            var lat = e.latLng.lat();
            var lng = e.latLng.lng();
            document.getElementById('boothLat').value = lat;
            document.getElementById('boothLng').value = lng;
            var label = document.getElementById('boothPosLabel');
            label.textContent = '선택된 위치: ' + lat.toFixed(6) + ', ' + lng.toFixed(6);
            label.style.color = 'var(--primary)';
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
        var errEl = document.getElementById('boothImageError');
        var allowed = ['.jpg', '.jpeg', '.png', '.gif'];
        var maxBytes = 10 * 1024 * 1024;
        if (!input.files || !input.files[0]) { errEl.style.display = 'none'; return true; }
        var file = input.files[0];
        var ext = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
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
        var imageInput = document.getElementById('boothImageFile');
        if (imageInput && imageInput.files && imageInput.files[0] && !validateBoothImage(imageInput)) {
            imageInput.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return false;
        }
        if (!document.getElementById('boothLat').value) {
            var label = document.getElementById('boothPosLabel');
            label.textContent = '⚠ 지도를 클릭하여 부스 위치를 먼저 선택해주세요.';
            label.style.color = 'var(--danger)';
            label.style.fontWeight = '600';
            document.getElementById('boothMapWrap').style.borderColor = 'var(--danger)';
            document.getElementById('boothMapWrap').scrollIntoView({ behavior: 'smooth', block: 'center' });
            return false;
        }
        return true;
    }

    function validateTimetable(form) {
        var festivalDate = document.getElementById('autoFestivalDate').value;
        if (!festivalDate) {
            var preview = document.getElementById('datePreview');
            preview.textContent = '⚠ 참여 아티스트 목록에서 날짜를 먼저 설정해주세요.';
            preview.style.color = 'var(--danger)';
            preview.style.fontWeight = '600';
            preview.style.borderColor = 'var(--danger)';
            preview.scrollIntoView({ behavior: 'smooth', block: 'center' });
            return false;
        }
        var start = form.querySelector('#startTime').value;
        var end = form.querySelector('#endTime').value;
        if (start && end && start >= end) {
            var err = document.getElementById('timeError');
            err.style.display = 'block';
            document.getElementById('startTime').style.borderColor = 'var(--danger)';
            document.getElementById('endTime').style.borderColor = 'var(--danger)';
            document.getElementById('endTime').focus();
            return false;
        }
        return true;
    }

    function validateCustomTimetable(form) {
        var start = form.querySelector('[name="startTime"]').value;
        var end = form.querySelector('[name="endTime"]').value;
        if (start && end && start >= end) {
            var err = document.getElementById('timeError');
            if (err) err.style.display = 'block';
            return false;
        }
        return true;
    }

    function clearTimeError() {
        var err = document.getElementById('timeError');
        if (err) err.style.display = 'none';
        document.getElementById('startTime').style.borderColor = '';
        document.getElementById('endTime').style.borderColor = '';
    }

    function onArtistChange(select) {
        var opt = select.options[select.selectedIndex];

        var stage = opt.getAttribute('data-stage') || '';
        document.getElementById('autoStageName').value = stage;
        var stagePreview = document.getElementById('stagePreview');
        if (stage) {
            stagePreview.textContent = stage;
            stagePreview.style.color = 'var(--primary)';
            stagePreview.style.fontWeight = '600';
        } else {
            stagePreview.textContent = '스테이지 미지정';
            stagePreview.style.color = 'var(--muted)';
            stagePreview.style.fontWeight = 'normal';
        }

        var date = opt.getAttribute('data-date') || '';
        document.getElementById('autoFestivalDate').value = date;
        var datePreview = document.getElementById('datePreview');
        if (date) {
            datePreview.textContent = date;
            datePreview.style.color = 'var(--text)';
            datePreview.style.fontWeight = '600';
            datePreview.style.borderColor = 'var(--border)';
        } else {
            datePreview.textContent = '⚠ 참여 날짜 미설정 — 위 목록에서 날짜를 먼저 저장하세요.';
            datePreview.style.color = 'var(--danger)';
            datePreview.style.fontWeight = '500';
            datePreview.style.borderColor = 'var(--danger)';
        }
    }

    /* ── 스테이지 색상 ── */
    var stageColors = [
        { bg: 'var(--primary)', light: '#eef2ff', border: '#c5d0fa' },
        { bg: '#e8590c', light: '#fff4e6', border: '#ffc9a0' },
        { bg: '#2b8a3e', light: '#ebfbee', border: '#b2f2bb' },
        { bg: '#be4bdb', light: '#f8f0fc', border: '#e599f7' },
        { bg: '#1098ad', light: '#e6fcf5', border: '#96f2d7' },
        { bg: '#f59f00', light: '#fff9db', border: '#ffe066' },
        { bg: '#d6336c', light: '#fff0f6', border: '#fcc2d7' },
        { bg: '#495057', light: '#f1f3f5', border: '#ced4da' },
    ];
    (function applyStageColors() {
        var stageMap = {};
        document.querySelectorAll('.stage-row[data-stage]').forEach(function (row, i) {
            var name = row.getAttribute('data-stage');
            var c = stageColors[i % stageColors.length];
            stageMap[name] = c;
            row.style.borderLeft = '4px solid ' + c.bg;
            row.style.background = c.light;
            row.style.borderColor = c.border;
            row.style.borderLeftColor = c.bg;
            row.querySelector('.stage-order-num').style.background = c.bg;
        });
        document.querySelectorAll('.badge-stage[data-stage]').forEach(function (badge) {
            var name = badge.getAttribute('data-stage');
            var c = stageMap[name];
            if (c) {
                badge.style.background = c.bg;
            }
        });
    })();

    /* ── 이벤트 리스너 ── */
    var artistSelect = document.getElementById('artistSelect');
    if (artistSelect) {
        artistSelect.addEventListener('change', function () {
            onArtistChange(this);
        });
    }

    var boothImageFile = document.getElementById('boothImageFile');
    if (boothImageFile) {
        boothImageFile.addEventListener('change', function () {
            validateBoothImage(this);
        });
    }

    var timetableForm = document.getElementById('timetable-add-form');
    if (timetableForm) {
        timetableForm.addEventListener('submit', function (e) {
            if (!validateTimetable(this)) e.preventDefault();
        });
    }

    var opsForm = document.getElementById('ops-add-form');
    if (opsForm) {
        opsForm.addEventListener('submit', function (e) {
            if (!validateCustomTimetable(this)) e.preventDefault();
        });
    }

    var boothForm = document.getElementById('booth-add-form');
    if (boothForm) {
        boothForm.addEventListener('submit', function (e) {
            if (!validateBoothPos()) e.preventDefault();
        });
    }

    var startTime = document.getElementById('startTime');
    var endTime   = document.getElementById('endTime');
    if (startTime) startTime.addEventListener('input', clearTimeError);
    if (endTime)   endTime.addEventListener('input', clearTimeError);

    /* Google Maps 초기화 (페이지 로드 후 콜백으로 호출됨) */
    window.initBoothMap = initBoothMap;
})();
