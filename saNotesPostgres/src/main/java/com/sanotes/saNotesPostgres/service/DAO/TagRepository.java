package com.sanotes.saNotesPostgres.service.DAO;

import com.sanotes.saNotesPostgres.service.model.TagModel;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface TagRepository extends CrudRepository<TagModel, Long> {
    Optional<TagModel> findByName(String name);


}
