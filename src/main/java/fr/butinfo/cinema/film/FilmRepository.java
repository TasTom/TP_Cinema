package fr.butinfo.cinema.film;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FilmRepository extends JpaRepository<Film, Long> {

    @Query("""
            select f from Film f
            where (:titre is null or lower(f.titre) like lower(concat('%', :titre, '%')))
              and (:genre is null or lower(f.genre) = lower(:genre))
            order by f.titre
            """)
    List<Film> rechercher(@Param("titre") String titre, @Param("genre") String genre);
}
