import {
  Configuration,
  GroupApi,
  GroupIdCreated,
  GroupRest,
  ItemApi,
  ItemCountByRight,
  ItemEntry,
  ItemInformation,
  RightApi,
  RightIdCreated,
  RightRest,
} from "@/generated-sources/openapi";

const configuration = new Configuration({
  basePath: window.location.origin + "/api/v1",
});

const loriItem = new ItemApi(configuration);
const loriRightApi = new RightApi(configuration);
const loriGroupApi = new GroupApi(configuration);

export default {
  /**
   * Group related calls.
   */
  addGroup(newGroup: GroupRest): Promise<GroupIdCreated> {
    return loriGroupApi.addGroup({
      body: newGroup,
    });
  },
  deleteGroup(groupId: string): Promise<void> {
    return loriGroupApi.deleteGroupById({
      id: groupId,
    });
  },
  getGroupById(groupId: string): Promise<GroupRest> {
    return loriGroupApi.getGroupById({
      id: groupId,
    });
  },
  getGroupList(
    offset: number,
    limit: number,
    idOnly: boolean
  ): Promise<Array<GroupRest>> {
    return loriGroupApi.getGroupList({
      offset: offset,
      limit: limit,
      idOnly: idOnly,
    });
  },
  updateGroup(g: GroupRest): Promise<void> {
    return loriGroupApi.updateGroup({
      body: g,
    });
  },
  getItemCountByRightId(rightId: string): Promise<ItemCountByRight> {
    return loriItem.getItemCountByRightId({ rightId: rightId });
  },
  updateRight(right: RightRest): Promise<void> {
    return loriRightApi.updateRight({ body: right });
  },
  addRight(right: RightRest): Promise<RightIdCreated> {
    return loriRightApi.addRight({ body: right });
  },
  addItemEntry(entry: ItemEntry): Promise<void> {
    return loriItem.addItemRelation({ body: entry });
  },
  deleteRight(rightId: string): Promise<void> {
    return loriRightApi.deleteRightById({ id: rightId });
  },
  deleteItemRelation(metadataId: string, rightId: string): Promise<void> {
    return loriItem.deleteItem({ metadataId: metadataId, rightId: rightId });
  },
  searchQuery(
    searchTerm: string,
    offset: number,
    limit: number,
    pageSize: number,
    filterPublicationDate: string | undefined,
    filterPublicationType: string | undefined,
    filterAccessState: string | undefined,
    filterTemporalValidity: string | undefined,
    filterStartDate: string | undefined,
    filterEndDate: string | undefined,
    filterFormalRule: string | undefined,
    filterValidOn: string | undefined,
    filterPaketSigel: string | undefined,
    filterZDBId: string | undefined
  ): Promise<ItemInformation> {
    return loriItem.getSearchResult({
      searchTerm: searchTerm,
      offset: offset,
      limit: limit,
      pageSize: pageSize,
      filterPublicationDate: filterPublicationDate,
      filterPublicationType: filterPublicationType,
      filterAccessState: filterAccessState,
      filterTemporalValidity: filterTemporalValidity,
      filterStartDate: filterStartDate,
      filterEndDate: filterEndDate,
      filterFormalRule: filterFormalRule,
      filterValidOn: filterValidOn,
      filterPaketSigel: filterPaketSigel,
      filterZDBId: filterZDBId,
    });
  },
};
