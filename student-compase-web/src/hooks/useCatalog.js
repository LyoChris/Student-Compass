import { useState, useEffect } from 'react'
import { catalogApi } from '../api/catalogApi'

export function useCatalog() {
  const [cities, setCities]                   = useState([])
  const [faculties, setFaculties]             = useState([])
  const [loadingCities, setLoadingCities]     = useState(true)
  const [loadingFaculties, setLoadingFaculties] = useState(false)

  useEffect(() => {
    catalogApi
      .getCities()
      .then(({ data }) => setCities(data))
      .catch(() => setCities([]))
      .finally(() => setLoadingCities(false))
  }, [])

  const loadFaculties = (cityId) => {
    setFaculties([])
    if (!cityId) return
    setLoadingFaculties(true)
    catalogApi
      .getFacultiesForCity(cityId)
      .then(({ data }) => setFaculties(data))
      .catch(() => setFaculties([]))
      .finally(() => setLoadingFaculties(false))
  }

  return { cities, faculties, loadingCities, loadingFaculties, loadFaculties }
}
