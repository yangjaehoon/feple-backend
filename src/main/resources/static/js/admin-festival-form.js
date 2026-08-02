(function () {
    var map, marker, geocoder;

    // appkey 미설정 또는 카카오 개발자 콘솔에 현재 접속 도메인이 플랫폼(Web)으로
    // 등록되어 있지 않으면 SDK 스크립트가 window.kakao를 정의하지 못한 채 끝난다 —
    // 그 상태로 kakao.maps.load()를 호출하면 전체 폼 스크립트가 죽으므로 방어한다.
    var kakaoAvailable = typeof kakao !== 'undefined' && !!kakao.maps;
    if (!kakaoAvailable) {
        console.error('[Kakao Maps] SDK 로드 실패 — appkey 또는 카카오 개발자 콘솔의 Web 플랫폼 도메인 등록을 확인하세요.');
        var mapEl = document.getElementById('kakaoMap');
        if (mapEl) mapEl.textContent = '지도를 불러올 수 없습니다. 잠시 후 다시 시도해주세요.';
    } else {
        kakao.maps.load(function () {
            var container = document.getElementById('kakaoMap');
            var options = {
                center: new kakao.maps.LatLng(37.5665, 126.9780),
                level: 5
            };
            map = new kakao.maps.Map(container, options);
            marker = new kakao.maps.Marker({ position: options.center, map: null });
            geocoder = new kakao.maps.services.Geocoder();

            kakao.maps.event.addListener(map, 'click', function (mouseEvent) {
                var latlng = mouseEvent.latLng;
                placeMarker(latlng.getLat(), latlng.getLng());
                geocoder.coord2Address(latlng.getLng(), latlng.getLat(), function (result, status) {
                    if (status === kakao.maps.services.Status.OK) {
                        var addr = result[0].road_address
                            ? result[0].road_address.address_name
                            : result[0].address.address_name;
                        document.getElementById('locationInput').value = addr;
                    }
                });
            });
        });
    }

    function placeMarker(lat, lng) {
        var pos = new kakao.maps.LatLng(lat, lng);
        marker.setPosition(pos);
        marker.setMap(map);
        map.setCenter(pos);
        document.getElementById('latInput').value = lat;
        document.getElementById('lngInput').value = lng;
    }

    function searchAddress() {
        if (!kakaoAvailable) return;
        var keyword = document.getElementById('addrSearch').value.trim();
        if (!keyword) return;
        var ps = new kakao.maps.services.Places();
        var addrErr = document.getElementById('addr-search-error');
        ps.keywordSearch(keyword, function (data, status) {
            if (status === kakao.maps.services.Status.OK) {
                var place = data[0];
                var lat = parseFloat(place.y);
                var lng = parseFloat(place.x);
                placeMarker(lat, lng);
                document.getElementById('locationInput').value = place.place_name;
                if (addrErr) addrErr.style.display = 'none';
            } else {
                geocoder.addressSearch(keyword, function (result, status) {
                    if (status === kakao.maps.services.Status.OK) {
                        var lat = parseFloat(result[0].y);
                        var lng = parseFloat(result[0].x);
                        placeMarker(lat, lng);
                        document.getElementById('locationInput').value = result[0].address_name;
                        if (addrErr) addrErr.style.display = 'none';
                    } else {
                        if (addrErr) addrErr.style.display = 'block';
                    }
                });
            }
        });
    }

    document.getElementById('addrSearch').addEventListener('keydown', function (e) {
        if (e.key === 'Enter') { e.preventDefault(); searchAddress(); }
    });

    document.getElementById('addr-search-btn').addEventListener('click', searchAddress);

    function filterArtists(query) {
        var q = query.toLowerCase();
        document.querySelectorAll('#artistList .artist-item').forEach(function (item) {
            item.style.display = item.dataset.name.includes(q) || (item.dataset.nameEn || '').includes(q) || (item.dataset.aliases || '').includes(q) ? '' : 'none';
        });
    }

    var startDateInput = document.getElementById('startDate');
    var endDateInput   = document.getElementById('endDate');
    if (startDateInput && endDateInput) {
        startDateInput.addEventListener('change', function () {
            var val = this.value;
            endDateInput.min = val;
            if (!endDateInput.value || endDateInput.value < val) {
                endDateInput.value = val;
            }
        });
    }

    var artistSearchInput = document.getElementById('artistSearch');
    if (artistSearchInput) {
        artistSearchInput.addEventListener('input', function () {
            filterArtists(this.value);
        });
    }

    document.querySelectorAll('[required]').forEach(function (field) {
        field.addEventListener('invalid', function () {
            var err = this.parentElement.querySelector('.field-error');
            if (err) err.style.display = 'block';
            this.style.borderColor = 'var(--danger)';
        });
        field.addEventListener('input', function () {
            if (this.value.trim()) {
                var err = this.parentElement.querySelector('.field-error');
                if (err) err.style.display = 'none';
                this.style.borderColor = '';
            }
        });
        field.addEventListener('change', function () {
            if (this.value) {
                var err = this.parentElement.querySelector('.field-error');
                if (err) err.style.display = 'none';
                this.style.borderColor = '';
            }
        });
    });
})();
