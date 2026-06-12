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

    function createBoothInfoWindow(booth) {
        var infoDiv = document.createElement('div');
        infoDiv.className = 'booth-info-popup';
        infoDiv.textContent = (booth.boothTypeName || '') + ' · ' + booth.name;
        if (booth.description) {
            var descEl = document.createElement('span');
            descEl.className = 'booth-info-desc';
            descEl.textContent = booth.description;
            infoDiv.appendChild(descEl);
        }
        return new google.maps.InfoWindow({ content: infoDiv });
    }

    function placeExistingBooths(map) {
        existingBooths.forEach(function (booth) {
            var marker = new google.maps.marker.AdvancedMarkerElement({
                position: { lat: booth.latitude, lng: booth.longitude },
                map: map,
                title: booth.name,
                content: createBoothEl(booth)
            });
            var infoWindow = createBoothInfoWindow(booth);
            marker.addListener('click', function () {
                infoWindow.open({ anchor: marker, map: map });
            });
        });
    }

    function setupMapClickToPlace(map) {
        map.addListener('click', function (e) {
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
                map: map,
                content: createSelectedEl()
            });
        });
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
        placeExistingBooths(boothMap);
        setupMapClickToPlace(boothMap);
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

    function applyStageRowStyle(row, c) {
        row.style.borderLeft      = '4px solid ' + c.bg;
        row.style.background      = c.light;
        row.style.borderColor     = c.border;
        row.style.borderLeftColor = c.bg;
        row.querySelector('.stage-order-num').style.background = c.bg;
    }

    function applyStageBadgeColors(colorMap) {
        document.querySelectorAll('.badge-stage[data-stage]').forEach(function (badge) {
            var c = colorMap[badge.getAttribute('data-stage')];
            if (c) {
                badge.style.background = c.bg;
            } else {
                badge.style.background = '#e9ecef';
                badge.style.color      = '#495057';
            }
        });
    }

    (function applyStageColors() {
        var colorMap = {};
        document.querySelectorAll('.stage-row[data-stage]').forEach(function (row, i) {
            var name = row.getAttribute('data-stage');
            var c    = stageColors[i % stageColors.length];
            colorMap[name] = c;
            applyStageRowStyle(row, c);
        });
        applyStageBadgeColors(colorMap);
    })();

    /* ── 슬롯 추가 폼 시간 유효성 검사 ── */
    document.querySelectorAll('[id^="slot-add-"]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            var formId  = this.id;
            var startEl = document.querySelector('[name="startTime"][form="' + formId + '"]');
            var endEl   = document.querySelector('[name="endTime"][form="' + formId + '"]');
            if (!startEl || !endEl || !startEl.value || !endEl.value) return;
            if (startEl.value >= endEl.value) {
                var cell = startEl.closest('.tt-slots-cell');
                if (cell) {
                    var errDiv = cell.querySelector('.slot-time-error');
                    if (errDiv) errDiv.style.display = 'block';
                }
                e.preventDefault();
            }
        });
    });

    document.querySelectorAll('.compact-input-xs').forEach(function (input) {
        input.addEventListener('input', function () {
            var cell = this.closest('.tt-slots-cell');
            if (cell) {
                var errDiv = cell.querySelector('.slot-time-error');
                if (errDiv) errDiv.style.display = 'none';
            }
        });
    });

    /* ── 운영 항목 추가 폼 시간 유효성 검사 ── */
    var opsForm = document.getElementById('ops-add-form');
    if (opsForm) {
        opsForm.addEventListener('submit', function (e) {
            var start = document.getElementById('opsStart');
            var end   = document.getElementById('opsEnd');
            if (start && end && start.value && end.value && start.value >= end.value) {
                document.getElementById('opsTimeError').style.display = 'block';
                e.preventDefault();
            }
        });
    }
    var opsStart = document.getElementById('opsStart');
    var opsEnd   = document.getElementById('opsEnd');
    if (opsStart) opsStart.addEventListener('input', function () { document.getElementById('opsTimeError').style.display = 'none'; });
    if (opsEnd)   opsEnd.addEventListener('input',   function () { document.getElementById('opsTimeError').style.display = 'none'; });

    /* ── 이벤트 리스너 ── */
    var boothImageFile = document.getElementById('boothImageFile');
    if (boothImageFile) {
        boothImageFile.addEventListener('change', function () {
            validateBoothImage(this);
        });
    }

    var boothForm = document.getElementById('booth-add-form');
    if (boothForm) {
        boothForm.addEventListener('submit', function (e) {
            if (!validateBoothPos()) e.preventDefault();
        });
    }

    /* ── 타임테이블 수정 모달 ── */
    function onTtEditArtistChange(select) {
        var nameInput = document.getElementById('tt-edit-artistName');
        if (select.value === '__direct__') {
            nameInput.style.display = 'block';
            nameInput.value = '';
            nameInput.focus();
        } else {
            nameInput.style.display = 'none';
            nameInput.value = select.value;
        }
    }

    function openTimetableEdit(btn) {
        var modal = document.getElementById('tt-edit-modal');
        var form  = document.getElementById('tt-edit-form');
        var festivalId  = btn.getAttribute('data-festival');
        var entryId     = btn.getAttribute('data-id');
        var artistName  = btn.getAttribute('data-artist') || '';

        form.action = '/admin/festivals/' + festivalId + '/timetable/' + entryId + '/update';
        document.getElementById('tt-edit-date').value      = btn.getAttribute('data-date')  || '';
        document.getElementById('tt-edit-start').value     = btn.getAttribute('data-start') || '';
        document.getElementById('tt-edit-end').value       = btn.getAttribute('data-end')   || '';
        document.getElementById('tt-edit-stageName').value = btn.getAttribute('data-stage') || '';
        document.getElementById('tt-edit-time-error').style.display = 'none';

        var select    = document.getElementById('tt-edit-artistSelect');
        var nameInput = document.getElementById('tt-edit-artistName');

        if (select) {
            var found = false;
            for (var i = 0; i < select.options.length; i++) {
                if (select.options[i].value !== '__direct__' && select.options[i].value === artistName) {
                    select.selectedIndex = i;
                    nameInput.value = artistName;
                    nameInput.style.display = 'none';
                    found = true;
                    break;
                }
            }
            if (!found) {
                for (var j = 0; j < select.options.length; j++) {
                    if (select.options[j].value === '__direct__') { select.selectedIndex = j; break; }
                }
                nameInput.value = artistName;
                nameInput.style.display = 'block';
            }
        } else {
            nameInput.value = artistName;
            nameInput.style.display = 'block';
        }

        modal.style.display = 'flex';
    }

    function closeTimetableEdit() {
        document.getElementById('tt-edit-modal').style.display = 'none';
    }

    document.getElementById('tt-edit-modal').addEventListener('click', function (e) {
        if (e.target === this) closeTimetableEdit();
    });

    document.getElementById('tt-edit-form').addEventListener('submit', function (e) {
        var start = document.getElementById('tt-edit-start').value;
        var end   = document.getElementById('tt-edit-end').value;
        if (start && end && start >= end) {
            document.getElementById('tt-edit-time-error').style.display = 'block';
            e.preventDefault();
        }
    });

    document.getElementById('tt-edit-start').addEventListener('input', function () {
        document.getElementById('tt-edit-time-error').style.display = 'none';
    });
    document.getElementById('tt-edit-end').addEventListener('input', function () {
        document.getElementById('tt-edit-time-error').style.display = 'none';
    });

    window.initBoothMap       = initBoothMap;
    window.openTimetableEdit  = openTimetableEdit;
    window.closeTimetableEdit = closeTimetableEdit;
    window.onTtEditArtistChange = onTtEditArtistChange;
})();
