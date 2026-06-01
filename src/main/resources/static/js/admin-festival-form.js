let map, marker, geocoder;

kakao.maps.load(function () {
    const container = document.getElementById('kakaoMap');
    const options = {
        center: new kakao.maps.LatLng(37.5665, 126.9780),
        level: 5
    };
    map = new kakao.maps.Map(container, options);
    marker = new kakao.maps.Marker({ position: options.center, map: null });
    geocoder = new kakao.maps.services.Geocoder();

    kakao.maps.event.addListener(map, 'click', function (mouseEvent) {
        const latlng = mouseEvent.latLng;
        placeMarker(latlng.getLat(), latlng.getLng());
        geocoder.coord2Address(latlng.getLng(), latlng.getLat(), function (result, status) {
            if (status === kakao.maps.services.Status.OK) {
                const addr = result[0].road_address
                    ? result[0].road_address.address_name
                    : result[0].address.address_name;
                document.getElementById('locationInput').value = addr;
            }
        });
    });
});

function placeMarker(lat, lng) {
    const pos = new kakao.maps.LatLng(lat, lng);
    marker.setPosition(pos);
    marker.setMap(map);
    map.setCenter(pos);
    document.getElementById('latInput').value = lat;
    document.getElementById('lngInput').value = lng;
}

function searchAddress() {
    const keyword = document.getElementById('addrSearch').value.trim();
    if (!keyword) return;
    const ps = new kakao.maps.services.Places();
    ps.keywordSearch(keyword, function (data, status) {
        if (status === kakao.maps.services.Status.OK) {
            const place = data[0];
            const lat = parseFloat(place.y);
            const lng = parseFloat(place.x);
            placeMarker(lat, lng);
            document.getElementById('locationInput').value = place.place_name;
        } else {
            geocoder.addressSearch(keyword, function (result, status) {
                if (status === kakao.maps.services.Status.OK) {
                    const lat = parseFloat(result[0].y);
                    const lng = parseFloat(result[0].x);
                    placeMarker(lat, lng);
                    document.getElementById('locationInput').value = result[0].address_name;
                } else {
                    alert('검색 결과가 없습니다.');
                }
            });
        }
    });
}

document.getElementById('addrSearch').addEventListener('keydown', function (e) {
    if (e.key === 'Enter') { e.preventDefault(); searchAddress(); }
});

function filterArtists(query) {
    const q = query.toLowerCase();
    document.querySelectorAll('#artistList .artist-item').forEach(item => {
        item.style.display = item.dataset.name.includes(q) ? '' : 'none';
    });
}

document.querySelectorAll('[required]').forEach(function(field) {
    field.addEventListener('invalid', function() {
        const err = this.parentElement.querySelector('.field-error');
        if (err) err.style.display = 'block';
        this.style.borderColor = '#e74c3c';
    });
    field.addEventListener('input', function() {
        if (this.value.trim()) {
            const err = this.parentElement.querySelector('.field-error');
            if (err) err.style.display = 'none';
            this.style.borderColor = '';
        }
    });
    field.addEventListener('change', function() {
        if (this.value) {
            const err = this.parentElement.querySelector('.field-error');
            if (err) err.style.display = 'none';
            this.style.borderColor = '';
        }
    });
});
