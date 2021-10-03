package com.sanotes.saNotesPostgres.Service.DAO;

import com.sanotes.saNotesPostgres.Service.Model.NoteBookModel;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface NoteBookRepository extends CrudRepository<NoteBookModel, Long> {
    @Query("select n from NoteBookModel n where n.name = ?1")
    List<NoteBookModel> findByName(String name);
}
