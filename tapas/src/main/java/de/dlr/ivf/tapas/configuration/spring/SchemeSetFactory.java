package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.trafficgeneration.SchemeProviderConfiguration;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.model.choice.DiscreteChoiceModel;
import de.dlr.ivf.tapas.model.choice.DiscreteDistribution;
import de.dlr.ivf.tapas.model.choice.DiscreteDistributionFactory;
import de.dlr.ivf.tapas.model.choice.DiscreteProbability;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.scheme.*;
import de.dlr.ivf.tapas.model.vehicle.TPS_CarCode;
import de.dlr.ivf.tapas.persistence.db.TPS_DB_IO;
import de.dlr.ivf.tapas.simulation.trafficgeneration.SchemeProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.util.*;

import static java.util.stream.Collectors.*;

@Configuration
public class SchemeSetFactory {

    private final TPS_DB_IO dbIo;

    @Autowired
    public SchemeSetFactory(TPS_DB_IO dbIo){
        this.dbIo = dbIo;
    }

    @Lazy
    @Bean
    public SchemeProvider schemeProvider(SchemeProviderConfiguration configuration,
                                         Map<Integer, List<DiscreteProbability<SchemeClass>>> schemeClassProbabilitiesByPersonGroup,
                                         PersonGroups personGroups){

        DiscreteDistributionFactory<SchemeClass> distributionFactory = new DiscreteDistributionFactory<>();

        Map<TPS_PersonGroup, DiscreteDistribution<SchemeClass>> schemeClassDistributions = new HashMap<>();

        for(TPS_PersonGroup personGroup : personGroups.getPersonGroups()){

            List<DiscreteProbability<SchemeClass>> probabilities = schemeClassProbabilitiesByPersonGroup.get(personGroup.getCode());

            if(probabilities == null){
                throw new IllegalArgumentException("No scheme class probabilities for person group:" + personGroup);
            }

            DiscreteDistribution<SchemeClass> distribution = distributionFactory.newNormalizedDiscreteDistribution(probabilities);
            schemeClassDistributions.put(personGroup, distribution);
        }

        DiscreteChoiceModel<SchemeClass> schemeClassChoiceModel = new DiscreteChoiceModel<>(configuration.schemeClassChoiceSeed());
        DiscreteChoiceModel<Scheme> schemeChoiceModel = new DiscreteChoiceModel<>(configuration.schemeChoiceSeed());

        return new SchemeProvider(schemeClassDistributions, schemeClassChoiceModel, schemeChoiceModel);
    }

    @Lazy
    @Bean
    public Map<Integer, List<DiscreteProbability<SchemeClass>>> schemeClassProbabilitiesByPersonGroup(
            Collection<SchemeClassDistributionDto> schemeClassDistributionDtos,
            Map<Integer, SchemeClass> schemeClassesByClassId){

        return schemeClassDistributionDtos
                .stream()
                .collect(
                        groupingBy(SchemeClassDistributionDto::getPersonGroup,
                                mapping(dto -> new DiscreteProbability<>(schemeClassesByClassId.get(dto.getSchemeClassId()),
                                                dto.getProbability()),
                                        toList()
                                )));
    }

    @Lazy
    @Bean
    public Map<Integer, SchemeClass> schemeClasses(SchemeProviderConfiguration configuration,
                                                   Collection<SchemeClassDto> schemeClassDtos,
                                                   Map<Integer, List<Scheme>> schemesByClassId) {

        Map<Integer, SchemeClass> schemeClasses = new HashMap<>(schemeClassDtos.size());

        for(SchemeClassDto schemeClassDto :schemeClassDtos){
            double mean = schemeClassDto.getAvgTravelTime() * configuration.timeSlotLength();
            double deviation = mean * schemeClassDto.getProcStdDev();

            List<Scheme> schemesInClass = schemesByClassId.get(schemeClassDto.getId());
            DiscreteDistributionFactory<Scheme> distributionFactory = new DiscreteDistributionFactory<>();
            DiscreteDistribution<Scheme> schemeDistribution = distributionFactory.newUniformNormalizedDiscreteDistribution(schemesInClass);

            SchemeClass schemeClass = new SchemeClass(schemeClassDto.getId(), mean, deviation,schemeDistribution);
            schemeClasses.put(schemeClass.id(), schemeClass);
        }

        return schemeClasses;
    }

    @Lazy
    @Bean
    public Map<Integer, List<Scheme>> schemesByClassID(Collection<SchemeDto> schemeDtos,
                                                             Map<Integer, Collection<Tour>> toursBySchemeId){

        return schemeDtos.stream()
                .map(dto -> new Scheme(dto.getId(), dto.getSchemeClassId(), toursBySchemeId.get(dto.getId())))
                .collect(groupingBy(Scheme::schemeClassId, toList()));
    }

    @Lazy
    @Bean
    public Map<Integer, Collection<Tour>> toursBySchemeId(SchemeProviderConfiguration configuration,
                                                          Collection<EpisodeDto> episodeDtos,
                                                          Activities activities){

        Map<Integer, Collection<EpisodeDto>> episodesBySchemeId = episodeDtos
                .stream()
                .collect(groupingBy(EpisodeDto::getSchemeId, toCollection(ArrayList::new)));

        Map<Integer, Collection<Tour>> toursBySchemeId = new HashMap<>();

        for (Map.Entry<Integer, Collection<EpisodeDto>> entry : episodesBySchemeId.entrySet()) {

            int schemeId = entry.getKey();
            Collection<EpisodeDto> episodesInScheme = entry.getValue();
            Collection<Tour> tours = generateToursFromEpisodes(configuration.timeSlotLength(), episodesInScheme,
                    activities, configuration.tripActivityCode());

            toursBySchemeId.put(schemeId, tours);
        }

        return toursBySchemeId;
    }

    public Collection<Tour> generateToursFromEpisodes(int timeSlotLength,
                                                      Collection<EpisodeDto> episodes,
                                                      Activities activities, int tripActivityCode) {

        List<EpisodeDto> sortedEpisodesInScheme = episodes
                .stream()
                .sorted(Comparator.comparingInt(EpisodeDto::getStart))
                .toList();

        Map<Integer, Tour> toursByTourId = new HashMap<>();

        for(int i = 1; i < sortedEpisodesInScheme.size() - 1; i = i+2){

            EpisodeDto startEpisode = sortedEpisodesInScheme.get(i-1);
            EpisodeDto tripEpisode = sortedEpisodesInScheme.get(i);
            EpisodeDto endEpisode = sortedEpisodesInScheme.get(i+1);

            validateEpisodesOrThrow(startEpisode, endEpisode, tripEpisode, tripActivityCode);

            Stay startStay = new Stay(startEpisode.getStart(), startEpisode.getDuration(), startEpisode.getActCodeZbe());
            Stay endStay  = new Stay(endEpisode.getStart(), endEpisode.getDuration(), endEpisode.getActCodeZbe());

            int tripPriority = activities.getActivity(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, endStay.activity())
                    .getCode(TPS_ActivityConstant.TPS_ActivityCodeType.PRIORITY);

            Trip trip = new Trip(startStay, endStay, tripEpisode.getStart(), tripEpisode.getDuration() * timeSlotLength, tripPriority);

            int tourNumber = tripEpisode.getTourNumber();
            toursByTourId.computeIfAbsent(tourNumber, tourId -> new Tour(tourId,new HashSet<>())).addTrip(trip);
        }

        return toursByTourId.values().stream().toList(); //remove the link to the map
    }

    private void validateEpisodesOrThrow(EpisodeDto start, EpisodeDto end, EpisodeDto trip, int tripActivityCode){
        if(start.getActCodeZbe() == tripActivityCode){
            throw new IllegalArgumentException("Start episode is a trip but it should be a stay.");
        }
        if(end.getActCodeZbe() == tripActivityCode){
            throw new IllegalArgumentException("End episode is a trip but it should be a stay.");
        }
        if(trip.getActCodeZbe() != tripActivityCode){
            throw new IllegalArgumentException("Trip episode is not of activity code " + trip.getActCodeZbe()+". Set tripActivityCode to "+trip.getActCodeZbe());
        }
    }

    @Lazy
    @Bean
    public Activities activities(Collection<TPS_ActivityConstant> activities) {

        return new Activities(activities);
    }

    @Lazy
    @Bean
    public Collection<TPS_ActivityConstant> activityConstants(Collection<ActivityDto> activityDtos){

        Collection<TPS_ActivityConstant> activities = new ArrayDeque<>(activityDtos.size());

        for(ActivityDto activityDto : activityDtos){

            String attribute = activityDto.getAttribute();

            var mctConstant = new TPS_InternalConstant<>(activityDto.getNameMct(), activityDto.getCodeMct(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeMct()));
            var tapasConstant = new TPS_InternalConstant<>(activityDto.getNameTapas(), activityDto.getCodeTapas(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeTapas()));
            var priorityConstant = new TPS_InternalConstant<>(activityDto.getNamePriority(), activityDto.getCodePriority(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypePriority()));
            var zbeConstant = new TPS_InternalConstant<>(activityDto.getNameZbe(), activityDto.getCodeZbe(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeZbe()));
            var votConstant = new TPS_InternalConstant<>(activityDto.getNameVot(), activityDto.getCodeVot(),
                    TPS_ActivityConstant.TPS_ActivityCodeType.valueOf(activityDto.getTypeVot()));

            TPS_ActivityConstant act = TPS_ActivityConstant.builder()
                    .id(activityDto.getId())
                    .internalConstant(mctConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.MCT,mctConstant)
                    .internalConstant(tapasConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.TAPAS,tapasConstant)
                    .internalConstant(priorityConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.PRIORITY, priorityConstant)
                    .internalConstant(zbeConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, zbeConstant)
                    .internalConstant(votConstant)
                    .internalAttribute(TPS_ActivityConstant.TPS_ActivityCodeType.VOT, votConstant)
                    .attribute(!(attribute == null || attribute.equals("null"))
                            ? TPS_ActivityConstant.TPS_ActivityConstantAttribute.valueOf(activityDto.getAttribute()) : TPS_ActivityConstant.TPS_ActivityConstantAttribute.DEFAULT)
                    .isFix(activityDto.isFix())
                    .isTrip(activityDto.isTrip())
                    .build();

            activities.add(act);
        }

        return activities;
    }

    /**
     * Creates a PersonGroups object based on the provided collection of PersonCodeDto objects.
     *
     * @param personCodeDtos A collection of PersonCodeDto objects containing information about each person group.
     * @return The created PersonGroups object.
     */
    @Lazy
    @Bean
    public PersonGroups personGroups(Collection<PersonCodeDto> personCodeDtos){

        PersonGroups.PersonGroupsBuilder personGroupsBuilder = PersonGroups.builder();

        for(PersonCodeDto personCodeDto : personCodeDtos) {
            TPS_PersonGroup personGroup = TPS_PersonGroup.builder()
                    .description(personCodeDto.getDescription())
                    .code(personCodeDto.getCode())
                    .personType(TPS_PersonType.valueOf(personCodeDto.getPersonType()))
                    .carCode(TPS_CarCode.getEnum(personCodeDto.getCodeCars()))
                    .hasChildCode(TPS_HasChildCode.valueOf(personCodeDto.getHasChild()))
                    .minAge(personCodeDto.getMinAge())
                    .maxAge(personCodeDto.getMaxAge())
                    .workStatus(TPS_WorkStatus.valueOf(personCodeDto.getWorkStatus()))
                    .sex(TPS_Sex.getEnum(personCodeDto.getCodeSex()))
                    .build();

            personGroupsBuilder.personGroup(personGroup.getCode(), personGroup);
        }

        return personGroupsBuilder.build();
    }

    @Lazy
    @Bean
    public Collection<ActivityDto> activityDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.activities(), ActivityDto.class, ActivityDto::new);
    }

    @Lazy
    @Bean
    public Collection<SchemeDto> schemeDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.schemes(), SchemeDto.class, SchemeDto::new);
    }

    @Lazy
    @Bean
    public Collection<SchemeClassDto> schemeClassDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.schemeClasses(), SchemeClassDto.class, SchemeClassDto::new);
    }

    @Lazy
    @Bean
    public Collection<EpisodeDto> episodeDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.episodes(), EpisodeDto.class, EpisodeDto::new);
    }

    @Lazy
    @Bean
    public Collection<SchemeClassDistributionDto> schemeClassDistributionDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.schemeClassDistributions(), SchemeClassDistributionDto.class, SchemeClassDistributionDto::new);
    }

    @Lazy
    @Bean
    public Collection<PersonCodeDto> personCodeDtos(SchemeProviderConfiguration configuration){
        return dbIo.readFromDb(configuration.personGroups(), PersonCodeDto.class, PersonCodeDto::new);
    }
}
