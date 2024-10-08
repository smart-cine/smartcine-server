package org.example.cinemamanagement.mapper;

import org.example.cinemamanagement.dto.film.FilmDTO;
import org.example.cinemamanagement.model.Film;
import org.example.cinemamanagement.model.Tag;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;

import java.util.stream.Collectors;

public class FilmMapper {
    public static FilmDTO toDTO(Film film) {
        TypeMap<Film, FilmDTO> typeMap = new ModelMapper().createTypeMap(Film.class, FilmDTO.class);
        FilmDTO filmDTO = typeMap.map(film);

        filmDTO.setCinemaProviderId(film.getCinemaProvider().getId());

        filmDTO.setTags(film.getTags().stream().map(Tag::getName)
                .collect(Collectors.toList()));

        return filmDTO;
    }
}
