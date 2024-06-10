package de.dlr.ivf.tapas.configuration.spring;

import de.dlr.ivf.tapas.configuration.json.runner.SchemeProviderConfiguration;
import de.dlr.ivf.tapas.dto.*;
import de.dlr.ivf.tapas.model.choice.DiscreteDistribution;
import de.dlr.ivf.tapas.model.choice.DiscreteDistributionFactory;
import de.dlr.ivf.tapas.model.choice.DiscreteProbability;
import de.dlr.ivf.tapas.model.constants.*;
import de.dlr.ivf.tapas.model.scheme.*;
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
    private final SchemeProviderConfiguration configuration;

    @Autowired
    public SchemeSetFactory(TPS_DB_IO dbIo, SchemeProviderConfiguration configuration){
        this.dbIo = dbIo;
        this.configuration = configuration;
    }

    @Bean
    public TPS_SchemeSet schemeSet(Collection<SchemeDto> schemeDtos,
                                   Collection<SchemeClassDto> schemeClassDtos,
                                   Collection<EpisodeDto> episodeDtos,
                                   Collection<SchemeClassDistributionDto> schemeClassDistributionDtos) {


        var schemeSet = dbIo.readSchemeSet(schemeClassDtos,schemeDtos,episodeDtos,schemeClassDistributionDtos, activityConstants, personGroups);
        return null;
    }

    @Bean
    public SchemeProvider schemeProvider(Collection<SchemeClassDistributionDto> schemeClassDistributionDtos,
                                  PersonGroups personGroups,
                                  Map<Integer, SchemeClass> schemeClassesByClassId){

        Map<Integer, List<DiscreteProbability<SchemeClass>>> schemeClassProbabilitiesByPersonGroup = schemeClassDistributionDtos
                .stream()
                .collect(
                        groupingBy(SchemeClassDistributionDto::getPersonGroup,
                        mapping(dto -> new DiscreteProbability<>(schemeClassesByClassId.get(dto.getSchemeClassId()), dto.getProbability()),
                        toList())));

        DiscreteDistributionFactory<SchemeClass> distributionFactory = new DiscreteDistributionFactory<>();

        Map<TPS_PersonGroup, DiscreteDistribution<SchemeClass>> schemeClassDistributions = new HashMap<>();

        for(TPS_PersonGroup personGroup : personGroups.getPersonGroups()){

            List<DiscreteProbability<SchemeClass>> probabilities = schemeClassProbabilitiesByPersonGroup.getOrDefault(personGroup, Collections.emptyList());


            DiscreteDistribution<SchemeClass> distribution = schemeClassDistributions.get(personGroup);
            List<DiscreteProbability<SchemeClass>> probabilities = schemeClassProbabilitiesByPersonGroup.get(personGroup);

        }


        for(Map.Entry<Integer, List<SchemeClassDistributionDto>> entry : schemeClassProbabilitiesByPersonGroup.entrySet()){

            TPS_PersonGroup personGroup = personGroups.getPersonGroupByCode(entry.getKey());
            List<SchemeClassDistributionDto> schemeClassProbabilities = entry.getValue();

            schemeClassProbabilities.sort(Comparator.comparing(SchemeClassDistributionDto::getSchemeClassId));

            DiscreteDistribution<SchemeClass> schemeClassDistribution =
                    generateSchemeClassDistribution(schemeClassProbabilities, schemeClassesByClassId);

            schemeClassDistributions.put(personGroup, distributionFactory.newNormalizedDiscreteDistribution(schemeClassDistribution));
        }


        return new SchemeProvider(configuration.getTimeSlotLength(), schemeSet, schemes, episodes, schemeClassDistributions, activities, personGroups);
    }

    @Bean
    public Map<TPS_PersonGroup, DiscreteDistribution<SchemeClass>> generateSchemeClassDistribution(){


    }

    /**
     * Generates a DiscreteDistribution of SchemeClasses based on the provided SchemeClassDistributionDtos and SchemeClasses.
     *
     * @param schemeClassDistributionDtos A list of SchemeClassDistributionDtos containing information about the SchemeClass distributions.
     * @param schemeClassesByClassId     A map of scheme classes with their corresponding scheme class ids.
     * @return The generated DiscreteDistribution of SchemeClasses.
     */
    private DiscreteDistribution<SchemeClass> generateSchemeClassDistribution(
            List<SchemeClassDistributionDto> schemeClassDistributionDtos,
            Map<Integer, SchemeClass> schemeClassesByClassId){

        DiscreteDistribution<SchemeClass> schemeClassDistribution = new DiscreteDistribution<>();

        for(SchemeClassDistributionDto schemeClassDistributionDto : schemeClassDistributionDtos){

            SchemeClass schemeClass = schemeClassesByClassId.get(schemeClassDistributionDto.getSchemeClassId());
            double probability = schemeClassDistributionDto.getProbability();

            schemeClassDistribution.addProbability(new DiscreteProbability<>(schemeClass, probability));
        }

        return schemeClassDistribution;
    }


    @Bean
    public Map<Integer, SchemeClass> schemeClasses(Collection<SchemeClassDto> schemeClassDtos,
                                                 Map<Integer, List<Scheme>> schemesByClassId) {

        Map<Integer, SchemeClass> schemeClasses = new HashMap<>(schemeClassDtos.size());

        for(SchemeClassDto schemeClassDto :schemeClassDtos){
            double mean = schemeClassDto.getAvgTravelTime() * 60;
            double deviation = mean * schemeClassDto.getProcStdDev();

            List<Scheme> schemesInClass = schemesByClassId.get(schemeClassDto.getId());
            DiscreteDistributionFactory<Scheme> distributionFactory = new DiscreteDistributionFactory<>();
            DiscreteDistribution<Scheme> schemeDistribution = distributionFactory.newUniformNormalizedDiscreteDistribution(schemesInClass);

            SchemeClass schemeClass = new SchemeClass(schemeClassDto.getId(), mean, deviation,schemeDistribution);
            schemeClasses.put(schemeClass.id(), schemeClass);
        }

        return schemeClasses;
    }

    @Bean
    public Map<Integer, List<Scheme>> schemesByClassID(Collection<SchemeDto> schemeDtos,
                                                             Map<Integer, Collection<Tour>> toursBySchemeId){

        return schemeDtos.stream()
                .map(dto -> new Scheme(dto.getId(), dto.getSchemeClassId(), toursBySchemeId.get(dto.getId())))
                .collect(groupingBy(Scheme::schemeClassId, toList()));
    }

    @Bean
    public Map<Integer, Collection<Tour>> toursBySchemeId(Collection<EpisodeDto> episodeDtos, Activities activities){

        Map<Integer, Collection<EpisodeDto>> episodesBySchemeId = episodeDtos
                .stream()
                .collect(groupingBy(EpisodeDto::getSchemeId, toCollection(ArrayList::new)));

        Map<Integer, Collection<Tour>> toursBySchemeId = new HashMap<>();

        for (Map.Entry<Integer, Collection<EpisodeDto>> entry : episodesBySchemeId.entrySet()) {

            int schemeId = entry.getKey();
            Collection<Tour> tours = generateToursFromEpisodes(episodeDtos, activities);

            toursBySchemeId.put(schemeId, tours);
        }

        return toursBySchemeId;
    }

    private Collection<Tour> generateToursFromEpisodes(Collection<EpisodeDto> episodes, Activities activities) {

        List<EpisodeDto> sortedEpisodesInScheme = episodes
                .stream()
                .sorted(Comparator.comparingInt(EpisodeDto::getStart))
                .toList();

        Map<Integer, Tour> toursByTourId = new HashMap<>();

        for(int i = 1; i < sortedEpisodesInScheme.size() - 1; i++){

            EpisodeDto startEpisode = sortedEpisodesInScheme.get(i-1);
            EpisodeDto tripEpisode = sortedEpisodesInScheme.get(i);
            EpisodeDto endEpisode = sortedEpisodesInScheme.get(i+1);

            Stay startStay = new Stay(startEpisode.getStart(), startEpisode.getDuration(), startEpisode.getActCodeZbe());
            Stay endStay  = new Stay(endEpisode.getStart(), endEpisode.getDuration(), endEpisode.getActCodeZbe());

            int tripPriority = activities.getActivity(TPS_ActivityConstant.TPS_ActivityCodeType.ZBE, endStay.activity())
                    .getCode(TPS_ActivityConstant.TPS_ActivityCodeType.PRIORITY);

            Trip trip = new Trip(startStay, endStay, tripEpisode.getStart(), tripEpisode.getDuration(), tripPriority);

            int tourNumber = tripEpisode.getTourNumber();
            toursByTourId.computeIfAbsent(tourNumber, tourId -> new Tour(tourId,new HashSet<>())).addTrip(trip);
        }

        return toursByTourId.values().stream().toList(); //remove the link to the map
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

    @Lazy
    @Bean
    public Collection<ActivityDto> activityDtos(){
        return dbIo.readFromDb(configuration.activities(), ActivityDto.class, ActivityDto::new);
    }

    @Bean
    public Collection<SchemeDto> schemeDtos(){
        return dbIo.readFromDb(configuration.schemes(), SchemeDto.class, SchemeDto::new);
    }

    @Bean
    public Collection<SchemeClassDto> schemeClassDtos(){
        return dbIo.readFromDb(configuration.schemeClasses(), SchemeClassDto.class, SchemeClassDto::new);
    }

    @Bean
    public Collection<EpisodeDto> episodeDtos(){
        return dbIo.readFromDb(configuration.episodes(), EpisodeDto.class, EpisodeDto::new);
    }

    @Bean
    public Collection<SchemeClassDistributionDto> schemeClassDistributionDtos(){
        return dbIo.readFromDb(configuration.schemeClassDistributions(), SchemeClassDistributionDto.class, SchemeClassDistributionDto::new);
    }
}
