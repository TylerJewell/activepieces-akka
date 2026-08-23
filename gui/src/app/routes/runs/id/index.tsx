import { FlowRun, PopulatedFlow } from '@activepieces/shared';
import { useQuery } from '@tanstack/react-query';
import { ReactFlowProvider } from '@xyflow/react';
import { useParams } from 'react-router-dom';

import { BuilderPage } from '@/app/builder';
import { BuilderStateProvider } from '@/app/builder/state/builder-state-provider';
import { LoadingSpinner } from '@/components/custom/spinner';
import { flowsApi, sampleDataHooks } from '@/features/flows';
import { useRunStream } from '@/lib/akka-feed';

const FlowRunPage = () => {
  const { runId, projectId } = useParams();
  // The run arrives on an open connection rather than by re-asking on a timer
  // (RENDERING.md R1). The flow it ran is fetched once, keyed by the version the
  // run names: it is the definition the run is read against, not the run's state.
  const run = useRunStream<FlowRun>(runId);
  const { data: flow, isLoading: isFlowLoading } = useQuery<PopulatedFlow, Error>({
    queryKey: ['flow', run?.flowId, run?.flowVersionId],
    queryFn: () => flowsApi.get(run!.flowId, { versionId: run!.flowVersionId }),
    enabled: run !== undefined,
    staleTime: Infinity,
  });
  const data = run !== undefined && flow !== undefined ? { run, flow } : undefined;
  const isLoading = run === undefined || isFlowLoading;

  const { data: sampleData, isLoading: isSampleDataLoading } =
    sampleDataHooks.useSampleDataForFlow(data?.flow?.version, projectId);

  const { data: sampleDataInput, isLoading: isSampleDataInputLoading } =
    sampleDataHooks.useSampleDataInputForFlow(data?.flow?.version, projectId);

  if (isLoading || isSampleDataLoading || isSampleDataInputLoading) {
    return (
      <div className="bg-background flex h-full w-full items-center justify-center ">
        <LoadingSpinner isLarge={true}></LoadingSpinner>
      </div>
    );
  }

  return (
    data && (
      <ReactFlowProvider>
        <BuilderStateProvider
          flow={data.flow}
          flowVersion={data.flow.version}
          readonly={true}
          hideTestWidget={false}
          run={data.run}
          outputSampleData={sampleData ?? {}}
          inputSampleData={sampleDataInput ?? {}}
        >
          <BuilderPage />
        </BuilderStateProvider>
      </ReactFlowProvider>
    )
  );
};

export { FlowRunPage };
