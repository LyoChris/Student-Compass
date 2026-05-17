package org.backendcompas.modules.radar.controller;

import org.backendcompas.modules.radar.model.City;
import org.backendcompas.modules.radar.model.Dorm;
import org.backendcompas.modules.radar.model.Faculty;
import org.backendcompas.modules.radar.repository.CityRepository;
import org.backendcompas.modules.radar.repository.DormRepository;
import org.backendcompas.modules.radar.repository.FacultyRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class OnboardingCatalogControllerTest {

    @Test
    void returnsCities() {
        CityRepository cityRepository = Mockito.mock(CityRepository.class);
        FacultyRepository facultyRepository = Mockito.mock(FacultyRepository.class);
        DormRepository dormRepository = Mockito.mock(DormRepository.class);
        OnboardingCatalogController controller = new OnboardingCatalogController(
                cityRepository, facultyRepository, dormRepository);

        City city = new City();
        city.setId(UUID.randomUUID());
        city.setName("Iasi");

        when(cityRepository.findAllByOrderByNameAsc()).thenReturn(List.of(city));

        var result = controller.getCities();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Iasi");
    }

    @Test
    void returnsFacultiesForCity() {
        CityRepository cityRepository = Mockito.mock(CityRepository.class);
        FacultyRepository facultyRepository = Mockito.mock(FacultyRepository.class);
        DormRepository dormRepository = Mockito.mock(DormRepository.class);
        OnboardingCatalogController controller = new OnboardingCatalogController(
                cityRepository, facultyRepository, dormRepository);

        UUID cityId = UUID.randomUUID();
        City city = new City();
        city.setId(cityId);
        city.setName("Iasi");

        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID());
        faculty.setCity(city);
        faculty.setName("FII");

        when(facultyRepository.findAllByCityIdOrderByNameAsc(cityId)).thenReturn(List.of(faculty));

        var result = controller.getFacultiesForCity(cityId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("FII");
        assertThat(result.get(0).cityId()).isEqualTo(cityId);
    }

    @Test
    void returnsDormsForCity() {
        CityRepository cityRepository = Mockito.mock(CityRepository.class);
        FacultyRepository facultyRepository = Mockito.mock(FacultyRepository.class);
        DormRepository dormRepository = Mockito.mock(DormRepository.class);
        OnboardingCatalogController controller = new OnboardingCatalogController(
                cityRepository, facultyRepository, dormRepository);

        UUID cityId = UUID.randomUUID();
        City city = new City();
        city.setId(cityId);
        city.setName("Iasi");

        Dorm dorm = new Dorm();
        dorm.setId(UUID.randomUUID());
        dorm.setCity(city);
        dorm.setName("Dorm 1");

        when(dormRepository.findAllByCityIdOrderByNameAsc(cityId)).thenReturn(List.of(dorm));

        var result = controller.getDormsForCity(cityId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Dorm 1");
        assertThat(result.get(0).cityId()).isEqualTo(cityId);
    }
}
