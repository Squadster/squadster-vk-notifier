package by.mercury.core.service.impl;

import by.mercury.core.dao.ScheduleDao;
import by.mercury.core.dao.SquadMemberDao;
import by.mercury.core.model.LessonModel;
import by.mercury.core.model.ScheduleModel;
import by.mercury.core.model.SquadMemberModel;
import by.mercury.core.model.UserModel;
import by.mercury.core.service.ScheduleService;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DefaultScheduleService implements ScheduleService {

    private static final String FONT_PATH = "src/main/resources/fonts/HelveticaRegular.ttf";
    private static final Font FONT = FontFactory.getFont(FONT_PATH, BaseFont.IDENTITY_H, true);
    private static final String PDF_PREFIX = "pdf_";
    private static final String PDF_EXTENSION = ".pdf";
    private static final String TITLE = "Расписание для %s на %s\n";
    private static final Integer COLUMNS = 6;

    private final ScheduleDao scheduleDao;
    
    private final SquadMemberDao squadMemberDao;

    public DefaultScheduleService(ScheduleDao scheduleDao, SquadMemberDao squadMemberDao) {
        this.scheduleDao = scheduleDao;
        this.squadMemberDao = squadMemberDao;
    }

    @Override
    public List<ScheduleModel> getScheduleForUser(UserModel user) {
        return squadMemberDao.findByUserId(user.getId())
                .map(SquadMemberModel::getSquadId)
                .map(scheduleDao::findAllBySquad)
                .orElse(Collections.emptyList());
                
    }

    @Override
    public File generateSchedule(List<ScheduleModel> schedules) {
        try {
            var pdfDocument = new Document();
            var tempFile = createTempFile();
            PdfWriter.getInstance(pdfDocument, new FileOutputStream(tempFile.getAbsolutePath()));
            pdfDocument.open();
            for (var schedule : schedules) {
                createTitle(schedule, pdfDocument);
                var sortedLessons = getLessons(schedule);
                writeLessons(sortedLessons, pdfDocument);
                pdfDocument.add(new Paragraph("\n\n"));
            }
            pdfDocument.close();
            return tempFile;
        } catch (DocumentException | IOException | IllegalArgumentException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private File createTempFile() throws IOException {
        return File.createTempFile(PDF_PREFIX, PDF_EXTENSION);
    }

    private void createTitle(ScheduleModel schedule, Document pdfDocument) throws DocumentException {
        var date = schedule.getDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        pdfDocument.add(new Paragraph(String.format(TITLE, schedule.getSquad(), date), FONT));
    }

    private List<LessonModel> getLessons(ScheduleModel schedule) {
        return schedule.getLessons().stream()
                .sorted(Comparator.comparing(LessonModel::getIndex))
                .collect(Collectors.toList());
    }

    private void writeLessons(List<LessonModel> lessons, Document pdfDocument) throws DocumentException {
        var table = new PdfPTable(COLUMNS);
        table.addCell(new Paragraph("№", FONT));
        table.addCell(new Paragraph("Название", FONT));
        table.addCell(new Paragraph("Тип", FONT));
        table.addCell(new Paragraph("Преподаватель", FONT));
        table.addCell(new Paragraph("Аудитория", FONT));
        table.addCell(new Paragraph("Заметка", FONT));
        lessons.forEach(lesson -> {
            table.addCell(new Paragraph(lesson.getIndex().toString(), FONT));
            table.addCell(new Paragraph(lesson.getName(), FONT));
            table.addCell(new Paragraph(lesson.getType(), FONT));
            table.addCell(new Paragraph(lesson.getTeacher(), FONT));
            table.addCell(new Paragraph(lesson.getClassroom(), FONT));
            table.addCell(new Paragraph(lesson.getNote(), FONT));
        });
        pdfDocument.add(table);
    }
}
