import api from './axiosInstance'

export const catalogApi = {
  /** GET /api/v1/catalog/cities → CatalogCityResponse[] */
  getCities: () =>
    api.get('/api/v1/catalog/cities'),

  /** GET /api/v1/catalog/cities/{cityId}/faculties → CatalogFacultyResponse[] */
  getFacultiesForCity: (cityId) =>
    api.get(`/api/v1/catalog/cities/${cityId}/faculties`),
}
