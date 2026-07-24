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
            marker.addEventListener('gmp-click', function () {
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

    /* Google Maps 초기화 (페이지 로드 후 콜백으로 호출됨) */
    window.initBoothMap = initBoothMap;
})();
